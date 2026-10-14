package com.playerpicked.chatgame.game;

import com.playerpicked.chatgame.game.puzzle.Puzzle;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ActiveRound {
    private final UUID id;
    private final Puzzle puzzle;
    private final long startedAtMillis;
    private final int durationSeconds;
    private final Set<String> participantNames;
    private final AtomicBoolean finished = new AtomicBoolean();

    private volatile ScheduledTask expiryTask;

    public ActiveRound(@NotNull UUID id, @NotNull Puzzle puzzle, long startedAtMillis, int durationSeconds,
                       @NotNull Set<String> participantNames) {
        this.id = id;
        this.puzzle = puzzle;
        this.startedAtMillis = startedAtMillis;
        this.durationSeconds = durationSeconds;
        this.participantNames = Set.copyOf(participantNames);
    }

    public @NotNull UUID id() {
        return id;
    }

    public @NotNull Puzzle puzzle() {
        return puzzle;
    }

    public @NotNull GameType type() {
        return puzzle.type();
    }

    public long startedAtMillis() {
        return startedAtMillis;
    }

    public int durationSeconds() {
        return durationSeconds;
    }

    public @NotNull @Unmodifiable Set<String> participantNames() {
        return participantNames;
    }

    public double secondsElapsed() {
        return Math.max(0, System.currentTimeMillis() - startedAtMillis) / 1000.0;
    }

    public long secondsRemaining() {
        long endsAtMillis = startedAtMillis + TimeUnit.SECONDS.toMillis(durationSeconds);
        return Math.max(0, TimeUnit.MILLISECONDS.toSeconds(endsAtMillis - System.currentTimeMillis()));
    }

    public boolean isFinished() {
        return finished.get();
    }

    void expireWith(@NotNull ScheduledTask task) {
        expiryTask = task;
        if (isFinished()) task.cancel();
    }

    boolean markFinished() {
        if (!finished.compareAndSet(false, true)) return false;

        ScheduledTask task = expiryTask;
        if (task != null) task.cancel();
        return true;
    }
}
