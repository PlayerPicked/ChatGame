package com.playerpicked.chatgame.game.puzzle;

import com.playerpicked.chatgame.game.Answer;
import com.playerpicked.chatgame.game.AnswerSource;
import com.playerpicked.chatgame.game.GameType;
import com.playerpicked.chatgame.game.GuessResult;
import com.playerpicked.chatgame.text.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public record CraftingPuzzle(@NotNull Material item, @NotNull @Unmodifiable List<Material> ingredients) implements Puzzle {

    public static @NotNull Optional<Puzzle> fromSeed(@NotNull List<String> seed) {
        if (seed.isEmpty()) return Optional.empty();
        return parse(seed.getFirst(), seed.subList(1, seed.size())).map(Puzzle.class::cast);
    }

    public static @NotNull Optional<CraftingPuzzle> parse(@NotNull String itemName, @NotNull List<String> ingredientNames) {
        Material item = Material.matchMaterial(itemName);
        if (item == null || !item.isItem()) return Optional.empty();

        List<Material> ingredients = new ArrayList<>();
        for (String ingredientName : ingredientNames) {
            Material ingredient = Material.matchMaterial(ingredientName);
            if (ingredient == null || !ingredient.isItem()) return Optional.empty();
            ingredients.add(ingredient);
        }
        return Optional.of(new CraftingPuzzle(item, List.copyOf(ingredients)));
    }

    @Override
    public @NotNull GameType type() {
        return GameType.CRAFTING;
    }

    @Override
    public @NotNull @Unmodifiable List<String> seed() {
        List<String> seed = new ArrayList<>();
        seed.add(item.name());
        ingredients.forEach(ingredient -> seed.add(ingredient.name()));
        return List.copyOf(seed);
    }

    @Override
    public @NotNull String hint() {
        return Arrays.stream(item.name().toLowerCase(Locale.ROOT).split("_"))
                .map(word -> word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    @Override
    public @NotNull String solution() {
        return hint();
    }

    @Override
    public @NotNull GuessResult guess(@NotNull UUID playerUuid, @NotNull Answer answer) {
        return isWorthSubmitting(answer) ? GuessResult.SOLVED : GuessResult.IGNORED;
    }

    @Override
    public boolean isWorthSubmitting(@NotNull Answer answer) {
        return answer.source() == AnswerSource.CRAFTING_TABLE && answer.text().equals(item.name());
    }

    @Override
    public @NotNull Component renderHint(@NotNull Messages messages) {
        return Component.translatable(item);
    }
}
