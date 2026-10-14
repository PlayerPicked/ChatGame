package com.playerpicked.chatgame.stats;

import com.playerpicked.chatgame.scheduler.AsyncExecutor;
import com.playerpicked.chatgame.storage.PlayerStatsRepository;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class StatsService {
    private final PlayerStatsRepository repository;
    private final StatsCache cache;
    private final AsyncExecutor async;

    public StatsService(@NotNull PlayerStatsRepository repository, @NotNull StatsCache cache, @NotNull AsyncExecutor async) {
        this.repository = repository;
        this.cache = cache;
        this.async = async;
    }

    public @NotNull CompletableFuture<Void> recordWin(@NotNull String playerName, double seconds) {
        return async.run(() -> repository.recordWin(playerName, seconds))
                .thenRun(() -> cache.refreshPlayer(playerName));
    }
}
