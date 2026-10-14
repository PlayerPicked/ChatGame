package com.playerpicked.chatgame.stats;

import com.playerpicked.chatgame.scheduler.AsyncExecutor;
import com.playerpicked.chatgame.storage.PlayerStatsRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class StatsCache implements Listener {
    public static final int LEADERBOARD_SIZE = 100;

    private final PlayerStatsRepository repository;
    private final AsyncExecutor async;
    private final Logger logger;
    private final Map<String, PlayerStats> onlinePlayers = new ConcurrentHashMap<>();

    private volatile Leaderboards leaderboards = new Leaderboards(List.of(), List.of());

    public StatsCache(@NotNull PlayerStatsRepository repository, @NotNull AsyncExecutor async, @NotNull Logger logger) {
        this.repository = repository;
        this.async = async;
        this.logger = logger;
    }

    public @NotNull PlayerStats of(@NotNull Player player) {
        return onlinePlayers.getOrDefault(player.getName(), PlayerStats.empty(player.getName()));
    }

    public @NotNull Optional<PlayerStats> mostWinsAt(int position) {
        return entryAt(leaderboards.mostWins(), position);
    }

    public @NotNull Optional<PlayerStats> fastestAt(int position) {
        return entryAt(leaderboards.fastest(), position);
    }

    public void refreshEverything() {
        List<String> onlineNames = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        async.run(() -> {
            leaderboards = new Leaderboards(repository.findMostWins(LEADERBOARD_SIZE), repository.findFastest(LEADERBOARD_SIZE));
            store(repository.findByNames(onlineNames));
        }).exceptionally(this::logFailure);
    }

    public void refreshPlayer(@NotNull String playerName) {
        async.run(() -> store(repository.findByNames(List.of(playerName)))).exceptionally(this::logFailure);
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        refreshPlayer(event.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        onlinePlayers.remove(event.getPlayer().getName());
    }

    private void store(@NotNull Collection<PlayerStats> stats) {
        stats.forEach(playerStats -> onlinePlayers.put(playerStats.name(), playerStats));
    }

    private Void logFailure(@NotNull Throwable error) {
        logger.warn("Could not refresh chat game statistics: {}", error.getMessage());
        return null;
    }

    private static @NotNull Optional<PlayerStats> entryAt(@NotNull List<PlayerStats> leaderboard, int position) {
        return position >= 1 && position <= leaderboard.size() ? Optional.of(leaderboard.get(position - 1)) : Optional.empty();
    }

    private record Leaderboards(@NotNull List<PlayerStats> mostWins, @NotNull List<PlayerStats> fastest) { }
}
