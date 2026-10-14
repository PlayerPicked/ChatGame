package com.playerpicked.chatgame.game.crafting;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RecipeMatcher {
    static final int GRID_SIZE = 3;

    private RecipeMatcher() { }

    public static boolean matches(@NotNull Recipe recipe, @NotNull ItemStack[] grid) {
        return switch (recipe) {
            case ShapedRecipe shaped -> matchesShaped(shaped, grid);
            case ShapelessRecipe shapeless -> matchesShapeless(shapeless, grid);
            default -> false;
        };
    }

    private static boolean matchesShaped(@NotNull ShapedRecipe recipe, @NotNull ItemStack[] grid) {
        String[] shape = recipe.getShape();
        Map<Character, RecipeChoice> choices = recipe.getChoiceMap();
        int width = Arrays.stream(shape).mapToInt(String::length).max().orElse(0);

        for (int rowOffset = 0; rowOffset <= GRID_SIZE - shape.length; rowOffset++) {
            for (int columnOffset = 0; columnOffset <= GRID_SIZE - width; columnOffset++) {
                if (matchesAt(shape, width, choices, grid, rowOffset, columnOffset, false)) return true;
                if (matchesAt(shape, width, choices, grid, rowOffset, columnOffset, true)) return true;
            }
        }
        return false;
    }

    private static boolean matchesAt(@NotNull String[] shape, int width, @NotNull Map<Character, RecipeChoice> choices,
                                     @NotNull ItemStack[] grid, int rowOffset, int columnOffset, boolean mirrored) {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int column = 0; column < GRID_SIZE; column++) {
                ItemStack placed = grid[row * GRID_SIZE + column];
                RecipeChoice expected = expectedAt(shape, width, choices, row - rowOffset, column - columnOffset, mirrored);

                if (expected == null ? !isEmpty(placed) : isEmpty(placed) || !expected.test(placed)) return false;
            }
        }
        return true;
    }

    private static @Nullable RecipeChoice expectedAt(@NotNull String[] shape, int width, @NotNull Map<Character, RecipeChoice> choices,
                                                     int row, int column, boolean mirrored) {
        if (row < 0 || row >= shape.length || column < 0 || column >= width) return null;

        int shapeColumn = mirrored ? width - 1 - column : column;
        String shapeRow = shape[row];
        if (shapeColumn >= shapeRow.length()) return null;
        return choices.get(shapeRow.charAt(shapeColumn));
    }

    private static boolean matchesShapeless(@NotNull ShapelessRecipe recipe, @NotNull ItemStack[] grid) {
        List<ItemStack> placed = new ArrayList<>(Arrays.stream(grid).filter(item -> !isEmpty(item)).map(Objects::requireNonNull).toList());
        List<RecipeChoice> required = recipe.getChoiceList();
        if (placed.size() != required.size()) return false;

        for (RecipeChoice choice : required) {
            int matchIndex = indexOfMatch(placed, choice);
            if (matchIndex < 0) return false;
            placed.remove(matchIndex);
        }
        return true;
    }

    private static int indexOfMatch(@NotNull List<ItemStack> placed, @NotNull RecipeChoice choice) {
        for (int index = 0; index < placed.size(); index++) {
            if (choice.test(placed.get(index))) return index;
        }
        return -1;
    }

    private static boolean isEmpty(@Nullable ItemStack item) {
        return item == null || item.isEmpty();
    }
}
