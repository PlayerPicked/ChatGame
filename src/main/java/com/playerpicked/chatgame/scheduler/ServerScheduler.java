package com.playerpicked.chatgame.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class ServerScheduler {
    private final Plugin plugin;

    public ServerScheduler(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    public void runForPlayer(@NotNull Player player, @NotNull Runnable task) {
        player.getScheduler().run(plugin, scheduledTask -> task.run(), null);
    }

    public void runForPlayerLater(@NotNull Player player, long delayTicks, @NotNull Runnable task) {
        player.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, Math.max(1, delayTicks));
    }

    public void runGlobal(@NotNull Runnable task) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    public @NotNull ScheduledTask runGlobalLater(long delayTicks, @NotNull Runnable task) {
        return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), Math.max(1, delayTicks));
    }

    public @NotNull ScheduledTask runGlobalTimer(long initialDelayTicks, long periodTicks, @NotNull Runnable task) {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), Math.max(1, initialDelayTicks), Math.max(1, periodTicks));
    }
}
