package com.playerpicked.chatgame.config;

import org.jetbrains.annotations.NotNull;

public record DatabaseSettings(
        @NotNull DatabaseType type,
        @NotNull String host,
        int port,
        @NotNull String databaseName,
        @NotNull String username,
        @NotNull String password,
        int poolSize
) {
    public @NotNull String mysqlJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + databaseName;
    }
}
