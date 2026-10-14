package com.playerpicked.chatgame.scheduler;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class AsyncExecutor {
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("ChatGame-worker-", 0).factory()
    );

    public @NotNull CompletableFuture<Void> run(@NotNull Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }

    public <T> @NotNull CompletableFuture<T> supply(@NotNull Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
