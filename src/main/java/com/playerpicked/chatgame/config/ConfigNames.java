package com.playerpicked.chatgame.config;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class ConfigNames {
    private ConfigNames() {
    }

    public static @NotNull String of(@NotNull Enum<?> constant) {
        return constant.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
