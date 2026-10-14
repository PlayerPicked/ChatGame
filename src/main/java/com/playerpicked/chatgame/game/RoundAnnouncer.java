package com.playerpicked.chatgame.game;

import com.playerpicked.chatgame.config.PluginConfig;
import com.playerpicked.chatgame.game.puzzle.Puzzle;
import com.playerpicked.chatgame.notification.ChatBroadcaster;
import com.playerpicked.chatgame.notification.SoundPlayer;
import com.playerpicked.chatgame.text.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class RoundAnnouncer {
    private final PluginConfig config;
    private final Messages messages;
    private final ChatBroadcaster broadcaster;
    private final SoundPlayer sounds;

    public RoundAnnouncer(@NotNull PluginConfig config, @NotNull Messages messages, @NotNull ChatBroadcaster broadcaster,
                          @NotNull SoundPlayer sounds) {
        this.config = config;
        this.messages = messages;
        this.broadcaster = broadcaster;
        this.sounds = sounds;
    }

    public void announceStart(@NotNull ActiveRound round) {
        sounds.playToEveryone(config.startSound());
        announceProgress(round);
    }

    public void announceProgress(@NotNull ActiveRound round) {
        broadcast(round, "announce", TagResolver.empty());
    }

    public void announceWin(@NotNull ActiveRound round, @NotNull String winnerName, double seconds) {
        broadcast(round, "won", TagResolver.resolver(
                Placeholder.unparsed("player", winnerName),
                Placeholder.unparsed("time", formatSeconds(seconds))
        ));
    }

    public void announceExpiry(@NotNull ActiveRound round) {
        broadcast(round, "expired", TagResolver.empty());
    }

    public static @NotNull String formatSeconds(double seconds) {
        return String.format(Locale.ROOT, "%.2f", seconds);
    }

    private void broadcast(@NotNull ActiveRound round, @NotNull String message, @NotNull TagResolver extra) {
        Puzzle puzzle = round.puzzle();
        TagResolver placeholders = TagResolver.resolver(
                extra,
                puzzle.extraPlaceholders(messages),
                Placeholder.component("puzzle", puzzle.renderHint(messages)),
                Placeholder.unparsed("answer", puzzle.solution())
        );
        broadcaster.broadcast(messages.lines(round.type().messageRoute(message), placeholders));
    }
}
