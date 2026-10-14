package com.playerpicked.chatgame.game;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class RoundTracker {
    private final List<Consumer<ActiveRound>> endListeners = new CopyOnWriteArrayList<>();

    private volatile ActiveRound current;
    private volatile long lastEndedAtMillis;

    public @NotNull Optional<ActiveRound> current() {
        return Optional.ofNullable(current);
    }

    public @NotNull Optional<ActiveRound> find(@NotNull UUID roundId) {
        ActiveRound round = current;
        return round != null && round.id().equals(roundId) ? Optional.of(round) : Optional.empty();
    }

    public long lastEndedAtMillis() {
        return lastEndedAtMillis;
    }

    public void onRoundEnd(@NotNull Consumer<ActiveRound> listener) {
        endListeners.add(listener);
    }

    synchronized void begin(@NotNull ActiveRound round) {
        ActiveRound previous = current;
        if (previous != null) end(previous);
        current = round;
    }

    synchronized boolean end(@NotNull ActiveRound round) {
        if (!round.markFinished()) return false;

        if (current == round) {
            current = null;
            lastEndedAtMillis = System.currentTimeMillis();
        }
        endListeners.forEach(listener -> listener.accept(round));
        return true;
    }
}
