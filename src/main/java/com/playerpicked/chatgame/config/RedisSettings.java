package com.playerpicked.chatgame.config;

import org.jetbrains.annotations.NotNull;

public record RedisSettings(
        boolean enabled,
        @NotNull String host,
        int port,
        @NotNull String password,
        int database,
        boolean ssl,
        int timeoutMillis,
        boolean master,
        @NotNull String serverId,
        @NotNull String channelPrefix
) {
    public boolean hasPassword() {
        return !password.isBlank();
    }

    public @NotNull String channel() {
        return channelPrefix + ":network";
    }
}
