package com.playerpicked.chatgame.game.puzzle;

import com.playerpicked.chatgame.game.Answer;
import com.playerpicked.chatgame.game.GameType;
import com.playerpicked.chatgame.game.GuessResult;
import com.playerpicked.chatgame.text.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.UUID;

public interface Puzzle {
    @NotNull GameType type();

    @NotNull @Unmodifiable List<String> seed();

    @NotNull String hint();

    @NotNull String solution();

    @NotNull GuessResult guess(@NotNull UUID playerUuid, @NotNull Answer answer);

    boolean isWorthSubmitting(@NotNull Answer answer);

    default @NotNull Component renderHint(@NotNull Messages messages) {
        return Component.text(hint());
    }

    default @NotNull TagResolver extraPlaceholders(@NotNull Messages messages) {
        return TagResolver.empty();
    }

    default @NotNull @Unmodifiable List<String> progress() {
        return List.of();
    }

    default void applyProgress(@NotNull List<String> progress) { }
}
