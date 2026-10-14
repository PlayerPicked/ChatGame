package com.playerpicked.chatgame.stats;

import org.jetbrains.annotations.NotNull;

public record PlayerStats(@NotNull String name, int wins, double fastestSeconds, int currentStreak, int bestStreak) {
    public static final double NO_TIME = 999999.99;

    public static @NotNull PlayerStats empty(@NotNull String name) {
        return new PlayerStats(name, 0, NO_TIME, 0, 0);
    }

    public boolean hasFastestTime() {
        return fastestSeconds < NO_TIME;
    }
}
