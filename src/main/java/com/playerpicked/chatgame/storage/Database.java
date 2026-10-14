package com.playerpicked.chatgame.storage;

import com.playerpicked.chatgame.config.DatabaseSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class Database implements AutoCloseable {
    private static final String MYSQL_SCHEMA = """
            CREATE TABLE IF NOT EXISTS chatgame_players (
                name VARCHAR(16) PRIMARY KEY,
                wins INTEGER NOT NULL DEFAULT 0,
                fastest_time DOUBLE NOT NULL DEFAULT 999999.99,
                current_streak INTEGER NOT NULL DEFAULT 0,
                best_streak INTEGER NOT NULL DEFAULT 0,
                INDEX idx_wins (wins),
                INDEX idx_fastest_time (fastest_time),
                INDEX idx_current_streak (current_streak),
                INDEX idx_best_streak (best_streak)
            )
            """;

    private static final String H2_SCHEMA = """
            CREATE TABLE IF NOT EXISTS chatgame_players (
                name VARCHAR(16) PRIMARY KEY,
                wins INTEGER NOT NULL DEFAULT 0,
                fastest_time DOUBLE NOT NULL DEFAULT 999999.99,
                current_streak INTEGER NOT NULL DEFAULT 0,
                best_streak INTEGER NOT NULL DEFAULT 0
            )
            """;

    private final HikariDataSource dataSource;
    private final SqlClient sql;

    private Database(@NotNull HikariDataSource dataSource) {
        this.dataSource = dataSource;
        this.sql = new SqlClient(dataSource);
    }

    public static @NotNull Database connect(@NotNull DatabaseSettings settings, @NotNull Path dataFolder) {
        return switch (settings.type()) {
            case MYSQL -> open(createMysqlPoolConfig(settings), MYSQL_SCHEMA);
            case H2 -> open(createH2PoolConfig(dataFolder), H2_SCHEMA);
        };
    }

    public @NotNull SqlClient sql() {
        return sql;
    }

    @Override
    public void close() {
        if (!dataSource.isClosed()) dataSource.close();
    }

    static @NotNull Database open(@NotNull HikariConfig poolConfig, @NotNull String schema) {
        Database database = new Database(new HikariDataSource(poolConfig));
        try {
            database.sql.update(schema);
        } catch (RuntimeException exception) {
            database.close();
            throw exception;
        }
        return database;
    }

    static @NotNull HikariConfig createH2PoolConfig(@NotNull Path dataFolder) {
        HikariConfig config = basePoolConfig();
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl("jdbc:h2:file:" + dataFolder.resolve("chatgame").toAbsolutePath() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        config.setMaximumPoolSize(4);
        return config;
    }

    private static @NotNull HikariConfig createMysqlPoolConfig(@NotNull DatabaseSettings settings) {
        HikariConfig config = basePoolConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(settings.mysqlJdbcUrl());
        config.setUsername(settings.username());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(settings.poolSize());

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        return config;
    }

    private static @NotNull HikariConfig basePoolConfig() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("ChatGame");
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        return config;
    }
}
