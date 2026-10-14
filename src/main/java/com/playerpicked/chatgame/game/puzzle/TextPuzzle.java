package com.playerpicked.chatgame.game.puzzle;

import com.playerpicked.chatgame.game.Answer;
import com.playerpicked.chatgame.game.GameType;
import com.playerpicked.chatgame.game.GuessResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record TextPuzzle(@NotNull GameType type, @NotNull String hint, @NotNull String solution) implements Puzzle {

    public static @NotNull Optional<Puzzle> fromSeed(@NotNull GameType type, @NotNull List<String> seed) {
        if (seed.size() != 2) return Optional.empty();
        return Optional.of(new TextPuzzle(type, seed.get(0), seed.get(1)));
    }

    @Override
    public @NotNull @Unmodifiable List<String> seed() {
        return List.of(hint, solution);
    }

    @Override
    public @NotNull GuessResult guess(@NotNull UUID playerUuid, @NotNull Answer answer) {
        if (!answer.isFromChat()) return GuessResult.IGNORED;
        return isWorthSubmitting(answer) ? GuessResult.SOLVED : GuessResult.WRONG;
    }

    @Override
    public boolean isWorthSubmitting(@NotNull Answer answer) {
        if (!answer.isFromChat()) return false;

        String attempt = answer.text().trim();
        return type == GameType.RANDOM_CHARACTERS ? attempt.equals(solution) : attempt.equalsIgnoreCase(solution);
    }
}
