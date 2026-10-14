package com.playerpicked.chatgame.game;

import com.playerpicked.chatgame.game.puzzle.Puzzle;
import com.playerpicked.chatgame.game.puzzle.PuzzleFactory;
import com.playerpicked.chatgame.network.NetworkMessage;
import com.playerpicked.chatgame.scheduler.ServerScheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Optional;

public final class RoundFollower {
    // if the master goes offline mid-round, the mirrored round still ends shortly after its timer
    private static final int MISSING_RESULT_GRACE_SECONDS = 10;
    private static final long TICKS_PER_SECOND = 20;

    private final PuzzleFactory puzzles;
    private final RoundTracker rounds;
    private final RoundAnnouncer announcer;
    private final RoundOutcomes outcomes;
    private final ServerScheduler scheduler;
    private final Logger logger;

    public RoundFollower(@NotNull PuzzleFactory puzzles, @NotNull RoundTracker rounds, @NotNull RoundAnnouncer announcer,
                         @NotNull RoundOutcomes outcomes, @NotNull ServerScheduler scheduler, @NotNull Logger logger) {
        this.puzzles = puzzles;
        this.rounds = rounds;
        this.announcer = announcer;
        this.outcomes = outcomes;
        this.scheduler = scheduler;
        this.logger = logger;
    }

    public void followStart(@NotNull NetworkMessage.RoundStarted started) {
        Optional<Puzzle> puzzle = puzzles.restore(started.game(), started.seed());
        if (puzzle.isEmpty()) {
            logger.warn("Ignoring a {} round from server {} with unreadable data {}", started.game(), started.sourceServerId(), started.seed());
            return;
        }

        ActiveRound round = new ActiveRound(started.roundId(), puzzle.get(), started.startedAtMillis(), started.durationSeconds(),
                GameService.onlinePlayerNames());
        rounds.begin(round);
        announcer.announceStart(round);

        long fallbackTicks = (started.durationSeconds() + MISSING_RESULT_GRACE_SECONDS) * TICKS_PER_SECOND;
        round.expireWith(scheduler.runGlobalLater(fallbackTicks, () -> rounds.end(round)));
    }

    public void followProgress(@NotNull NetworkMessage.RoundProgressed progressed) {
        rounds.find(progressed.roundId()).ifPresent(round -> {
            round.puzzle().applyProgress(progressed.progress());
            announcer.announceProgress(round);
        });
    }

    public void followWin(@NotNull NetworkMessage.RoundWon won) {
        rounds.find(won.roundId())
                .filter(rounds::end)
                .ifPresent(round -> outcomes.won(round, new Contestant(won.winnerUuid(), won.winnerName()), won.seconds()));
    }

    public void followExpiry(@NotNull NetworkMessage.RoundExpired expired) {
        rounds.find(expired.roundId()).filter(rounds::end).ifPresent(outcomes::expired);
    }

    public void followCancel(@NotNull NetworkMessage.RoundCancelled cancelled) {
        rounds.find(cancelled.roundId()).ifPresent(rounds::end);
    }
}
