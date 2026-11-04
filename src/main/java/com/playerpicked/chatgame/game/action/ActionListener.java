package com.playerpicked.chatgame.game.action;

import com.playerpicked.chatgame.game.ActiveRound;
import com.playerpicked.chatgame.game.GameService;
import com.playerpicked.chatgame.text.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ActionListener implements Listener {
    public static final String PROGRESS_MESSAGE = "actions.progress";

    private final GameService games;
    private final Messages messages;

    public ActionListener(@NotNull GameService games, @NotNull Messages messages) {
        this.games = games;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(@NotNull BlockBreakEvent event) {
        ActionPuzzle puzzle = currentPuzzle();

        if (puzzle == null) return;
        if (puzzle.wasPlacedThisRound(keyOf(event.getBlock()))) return;

        progress(puzzle, event.getPlayer(), ActionKind.BREAK_BLOCK, event.getBlock().getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(@NotNull BlockPlaceEvent event) {
        ActionPuzzle puzzle = currentPuzzle();
        if (puzzle == null) return;

        boolean freshSpot = puzzle.rememberPlacement(keyOf(event.getBlockPlaced()));
        if (freshSpot) progress(puzzle, event.getPlayer(), ActionKind.PLACE_BLOCK, event.getBlockPlaced().getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobDeath(@NotNull EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim instanceof Player) return;

        Player killer = victim.getKiller();
        if (killer != null) progress(killer, ActionKind.KILL_MOB, victim.getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) progress(killer, ActionKind.KILL_PLAYER, victim.getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(@NotNull PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item caught)) return;
        progress(event.getPlayer(), ActionKind.CATCH_FISH, caught.getItemStack().getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(@NotNull PlayerItemConsumeEvent event) {
        progress(event.getPlayer(), ActionKind.CONSUME_ITEM, event.getItem().getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(@NotNull EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player breeder) progress(breeder, ActionKind.BREED_ANIMAL, event.getEntity().getType().name());
    }

    private void progress(@NotNull Player player, @NotNull ActionKind action, @NotNull String target) {
        ActionPuzzle puzzle = currentPuzzle();
        if (puzzle != null) progress(puzzle, player, action, target);
    }

    private void progress(@NotNull ActionPuzzle puzzle, @NotNull Player player, @NotNull ActionKind action, @NotNull String target) {
        if (!puzzle.matches(action, target)) return;

        int count = puzzle.record(player.getUniqueId());
        if (count >= puzzle.amount()) {
            games.submit(player, puzzle.completedAnswer());
            return;
        }

        player.sendActionBar(messages.single(PROGRESS_MESSAGE,
                Placeholder.component("puzzle", puzzle.renderHint(messages)),
                Placeholder.unparsed("count", String.valueOf(count)),
                Placeholder.unparsed("amount", String.valueOf(puzzle.amount()))));
    }

    private @Nullable ActionPuzzle currentPuzzle() {
        ActiveRound round = games.currentRound().orElse(null);
        return round != null && !round.isFinished() && round.puzzle() instanceof ActionPuzzle puzzle ? puzzle : null;
    }

    private static @NotNull String keyOf(@NotNull Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}
