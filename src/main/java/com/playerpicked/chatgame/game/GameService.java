package com.playerpicked.chatgame.game;

import com.playerpicked.chatgame.config.PluginConfig;
import com.playerpicked.chatgame.config.RedisSettings;
import com.playerpicked.chatgame.game.puzzle.Puzzle;
import com.playerpicked.chatgame.game.puzzle.PuzzleFactory;
import com.playerpicked.chatgame.network.NetworkBus;
import com.playerpicked.chatgame.network.NetworkMessage;
import com.playerpicked.chatgame.scheduler.ServerScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class GameService {
    private static final long TICKS_PER_SECOND = 20;

    private final PluginConfig config;
    private final PuzzleFactory puzzles;
    private final RoundTracker rounds;
    private final RoundAnnouncer announcer;
    private final RoundOutcomes outcomes;
    private final NetworkBus network;
    private final ServerScheduler scheduler;
    private final boolean authority;
    private final String serverId;

    public GameService(@NotNull PluginConfig config, @NotNull PuzzleFactory puzzles, @NotNull RoundTracker rounds,
                       @NotNull RoundAnnouncer announcer, @NotNull RoundOutcomes outcomes, @NotNull NetworkBus network,
                       @NotNull ServerScheduler scheduler, @NotNull RedisSettings redis) {
        this.config = config;
        this.puzzles = puzzles;
        this.rounds = rounds;
        this.announcer = announcer;
        this.outcomes = outcomes;
        this.network = network;
        this.scheduler = scheduler;
        this.authority = !redis.enabled() || redis.master();
        this.serverId = redis.serverId();
    }

    public boolean isAuthority() {
        return authority;
    }

    public @NotNull Optional<ActiveRound> currentRound() {
        return rounds.current();
    }

    public @NotNull StartResult start(@NotNull GameType type) {
        if (!authority) return StartResult.NOT_MASTER_SERVER;

        Optional<Puzzle> puzzle = puzzles.create(type);
        if (puzzle.isEmpty()) return StartResult.NOT_CONFIGURED;

        rounds.current().ifPresent(this::cancel);

        int durationSeconds = config.game(type.configSection()).roundSeconds();
        ActiveRound round = new ActiveRound(UUID.randomUUID(), puzzle.get(), System.currentTimeMillis(), durationSeconds,
                onlinePlayerNames());
        rounds.begin(round);

        network.publish(new NetworkMessage.RoundStarted(serverId, round.id(), type, puzzle.get().seed(),
                round.startedAtMillis(), durationSeconds));
        announcer.announceStart(round);
        round.expireWith(scheduler.runGlobalLater(durationSeconds * TICKS_PER_SECOND, () -> expire(round)));
        return StartResult.STARTED;
    }

    public void cancelCurrentRound() {
        if (authority) rounds.current().ifPresent(this::cancel);
    }

    public void submit(@NotNull Player player, @NotNull Answer answer) {
        Optional<ActiveRound> current = rounds.current();
        if (current.isEmpty()) return;

        ActiveRound round = current.get();
        if (authority) {
            evaluate(round, Contestant.of(player), answer);
        } else if (round.puzzle().isWorthSubmitting(answer)) {
            network.publish(new NetworkMessage.AnswerSubmitted(serverId, round.id(), player.getUniqueId(), player.getName(),
                    answer.text(), answer.source()));
        }
    }

    public void acceptRemoteAnswer(@NotNull NetworkMessage.AnswerSubmitted submitted) {
        if (!authority) return;

        rounds.find(submitted.roundId()).ifPresent(round -> evaluate(round,
                new Contestant(submitted.playerUuid(), submitted.playerName()),
                new Answer(submitted.answer(), submitted.source())));
    }

    private void evaluate(@NotNull ActiveRound round, @NotNull Contestant contestant, @NotNull Answer answer) {
        // chat events arrive on many threads; one guess at a time keeps hangman progress messages in order
        synchronized (round) {
            if (round.isFinished()) return;

            switch (round.puzzle().guess(contestant.uuid(), answer)) {
                case IGNORED, WRONG -> { }
                case PROGRESSED -> {
                    network.publish(new NetworkMessage.RoundProgressed(serverId, round.id(), round.puzzle().progress()));
                    announcer.announceProgress(round);
                }
                case SOLVED -> win(round, contestant);
                case LOST -> expire(round);
            }
        }
    }

    private void win(@NotNull ActiveRound round, @NotNull Contestant winner) {
        if (!rounds.end(round)) return;

        double seconds = round.secondsElapsed();
        network.publish(new NetworkMessage.RoundWon(serverId, round.id(), winner.uuid(), winner.name(), seconds));
        outcomes.won(round, winner, seconds);
    }

    private void expire(@NotNull ActiveRound round) {
        if (!rounds.end(round)) return;

        network.publish(new NetworkMessage.RoundExpired(serverId, round.id()));
        outcomes.expired(round);
    }

    private void cancel(@NotNull ActiveRound round) {
        if (rounds.end(round)) network.publish(new NetworkMessage.RoundCancelled(serverId, round.id()));
    }

    static @NotNull Set<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toUnmodifiableSet());
    }
}
