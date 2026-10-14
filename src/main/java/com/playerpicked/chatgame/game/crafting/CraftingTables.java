package com.playerpicked.chatgame.game.crafting;

import com.playerpicked.chatgame.game.ActiveRound;
import com.playerpicked.chatgame.game.Answer;
import com.playerpicked.chatgame.game.AnswerSource;
import com.playerpicked.chatgame.game.GameService;
import com.playerpicked.chatgame.game.puzzle.CraftingPuzzle;
import com.playerpicked.chatgame.scheduler.ServerScheduler;
import com.playerpicked.chatgame.text.MessageKey;
import com.playerpicked.chatgame.text.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class CraftingTables implements Listener {
    private static final int RESULT_SLOT = 0;
    private static final int FIRST_GRID_SLOT = 1;
    private static final int LAST_GRID_SLOT = 9;
    private static final Set<InventoryAction> ALLOWED_GRID_ACTIONS = Set.of(
            InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF, InventoryAction.PICKUP_ONE, InventoryAction.PICKUP_SOME,
            InventoryAction.PLACE_ALL, InventoryAction.PLACE_ONE, InventoryAction.PLACE_SOME, InventoryAction.SWAP_WITH_CURSOR
    );

    private final GameService games;
    private final Messages messages;
    private final ServerScheduler scheduler;

    public CraftingTables(@NotNull GameService games, @NotNull Messages messages, @NotNull ServerScheduler scheduler) {
        this.games = games;
        this.messages = messages;
        this.scheduler = scheduler;
    }

    public boolean open(@NotNull Player player) {
        ActiveRound round = games.currentRound().orElse(null);
        if (round == null || !(round.puzzle() instanceof CraftingPuzzle puzzle)) return false;

        CraftingTableHolder holder = new CraftingTableHolder(round.id(), puzzle);
        Inventory inventory = Bukkit.createInventory(holder, InventoryType.WORKBENCH,
                messages.single(MessageKey.CRAFTING_MENU_TITLE, Placeholder.component("item", puzzle.renderHint(messages))));
        holder.attach(inventory);

        List<ItemStack> ingredients = new ArrayList<>(puzzle.ingredients().stream().map(ItemStack::of).toList());
        Collections.shuffle(ingredients);
        for (int index = 0; index < Math.min(ingredients.size(), LAST_GRID_SLOT); index++) {
            inventory.setItem(FIRST_GRID_SLOT + index, ingredients.get(index));
        }

        scheduler.runForPlayer(player, () -> player.openInventory(inventory));
        return true;
    }

    public void closeAll(@NotNull ActiveRound endedRound) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            scheduler.runForPlayer(player, () -> {
                CraftingTableHolder holder = holderOf(player.getOpenInventory().getTopInventory());
                if (holder != null && holder.roundId().equals(endedRound.id())) player.closeInventory();
            });
        }
    }

    public void closeAllNow() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (holderOf(player.getOpenInventory().getTopInventory()) != null) player.closeInventory();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(@NotNull InventoryClickEvent event) {
        Inventory table = event.getView().getTopInventory();
        CraftingTableHolder holder = holderOf(table);
        if (holder == null || !(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        boolean gridChange = slot >= FIRST_GRID_SLOT && slot <= LAST_GRID_SLOT && ALLOWED_GRID_ACTIONS.contains(event.getAction());
        if (!gridChange) event.setCancelled(true);

        if (slot == RESULT_SLOT) submitResult(player, holder, event.getCurrentItem());
        else if (gridChange) scheduler.runForPlayerLater(player, 1, () -> updateResult(table, holder.puzzle()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(@NotNull InventoryDragEvent event) {
        Inventory table = event.getView().getTopInventory();
        CraftingTableHolder holder = holderOf(table);
        if (holder == null || !(event.getWhoClicked() instanceof Player player)) return;

        boolean onlyGridSlots = event.getRawSlots().stream().allMatch(slot -> slot >= FIRST_GRID_SLOT && slot <= LAST_GRID_SLOT);
        if (!onlyGridSlots) {
            event.setCancelled(true);
            return;
        }
        scheduler.runForPlayerLater(player, 1, () -> updateResult(table, holder.puzzle()));
    }

    @EventHandler
    public void onClose(@NotNull InventoryCloseEvent event) {
        Inventory table = event.getView().getTopInventory();
        if (holderOf(table) == null) return;

        // the ingredients were handed out for free; nothing may leave the table with the player
        table.clear();
        event.getPlayer().setItemOnCursor(null);
    }

    private void submitResult(@NotNull Player player, @NotNull CraftingTableHolder holder, @Nullable ItemStack result) {
        if (result == null || result.getType() != holder.puzzle().item()) return;
        games.submit(player, new Answer(holder.puzzle().item().name(), AnswerSource.CRAFTING_TABLE));
    }

    private static void updateResult(@NotNull Inventory table, @NotNull CraftingPuzzle puzzle) {
        ItemStack[] grid = new ItemStack[LAST_GRID_SLOT];
        for (int slot = FIRST_GRID_SLOT; slot <= LAST_GRID_SLOT; slot++) grid[slot - FIRST_GRID_SLOT] = table.getItem(slot);

        boolean crafted = Bukkit.getRecipesFor(ItemStack.of(puzzle.item())).stream()
                .anyMatch(recipe -> RecipeMatcher.matches(recipe, grid));
        table.setItem(RESULT_SLOT, crafted ? ItemStack.of(puzzle.item()) : null);
    }

    private static @Nullable CraftingTableHolder holderOf(@NotNull Inventory inventory) {
        return inventory.getHolder(false) instanceof CraftingTableHolder holder ? holder : null;
    }
}
