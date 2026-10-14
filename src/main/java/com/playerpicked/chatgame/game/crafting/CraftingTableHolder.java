package com.playerpicked.chatgame.game.crafting;

import com.playerpicked.chatgame.game.puzzle.CraftingPuzzle;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class CraftingTableHolder implements InventoryHolder {
    private final UUID roundId;
    private final CraftingPuzzle puzzle;
    private Inventory inventory;

    CraftingTableHolder(@NotNull UUID roundId, @NotNull CraftingPuzzle puzzle) {
        this.roundId = roundId;
        this.puzzle = puzzle;
    }

    public @NotNull UUID roundId() {
        return roundId;
    }

    public @NotNull CraftingPuzzle puzzle() {
        return puzzle;
    }

    void attach(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
