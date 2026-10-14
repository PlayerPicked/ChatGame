package com.playerpicked.chatgame.game.puzzle;

import com.playerpicked.chatgame.game.Answer;
import com.playerpicked.chatgame.game.GameType;
import com.playerpicked.chatgame.game.GuessResult;
import com.playerpicked.chatgame.text.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record RangePuzzle(int target, int min, int max) implements Puzzle {

    public static @NotNull Optional<Puzzle> fromSeed(@NotNull List<String> seed) {
        if (seed.size() != 3) return Optional.empty();
        return parse(seed.get(0)).flatMap(target -> parse(seed.get(1)).flatMap(min -> parse(seed.get(2))
                .map(max -> new RangePuzzle(target, min, max))));
    }

    @Override
    public @NotNull GameType type() {
        return GameType.RANGE;
    }

    @Override
    public @NotNull @Unmodifiable List<String> seed() {
        return List.of(String.valueOf(target), String.valueOf(min), String.valueOf(max));
    }

    @Override
    public @NotNull String hint() {
        return min + " ↔ " + max;
    }

    @Override
    public @NotNull String solution() {
        return String.valueOf(target);
    }

    @Override
    public @NotNull GuessResult guess(@NotNull UUID playerUuid, @NotNull Answer answer) {
        if (!answer.isFromChat()) return GuessResult.IGNORED;
        return parse(answer.text()).map(guess -> guess == target ? GuessResult.SOLVED : GuessResult.WRONG)
                .orElse(GuessResult.IGNORED);
    }

    @Override
    public boolean isWorthSubmitting(@NotNull Answer answer) {
        return answer.isFromChat() && parse(answer.text()).filter(guess -> guess == target).isPresent();
    }

    @Override
    public @NotNull TagResolver extraPlaceholders(@NotNull Messages messages) {
        return TagResolver.resolver(
                Placeholder.unparsed("min", String.valueOf(min)),
                Placeholder.unparsed("max", String.valueOf(max))
        );
    }

    private static @NotNull Optional<Integer> parse(@NotNull String text) {
        try {
            return Optional.of(Integer.parseInt(text.trim()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
