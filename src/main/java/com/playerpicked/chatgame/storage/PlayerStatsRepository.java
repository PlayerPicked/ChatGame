package com.playerpicked.chatgame.storage;

import com.playerpicked.chatgame.stats.PlayerStats;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PlayerStatsRepository {
    private static final String ENSURE_PLAYER = "INSERT IGNORE INTO chatgame_players (name) VALUES (?)";

    private final SqlClient sql;

    public PlayerStatsRepository(@NotNull SqlClient sql) {
        this.sql = sql;
    }

    public void recordWin(@NotNull String playerName, double seconds) {
        sql.inTransaction(transaction -> {
            transaction.update(ENSURE_PLAYER, playerName);
            transaction.update("UPDATE chatgame_players SET wins = wins + 1, fastest_time = LEAST(fastest_time, ?) WHERE name = ?",
                    seconds, playerName);
        });
    }

    public int incrementStreak(@NotNull String playerName) {
        // two statements, because MySQL and H2 disagree on whether SET sees the already updated current_streak
        sql.inTransaction(transaction -> {
            transaction.update(ENSURE_PLAYER, playerName);
            transaction.update("UPDATE chatgame_players SET current_streak = current_streak + 1 WHERE name = ?", playerName);
            transaction.update("UPDATE chatgame_players SET best_streak = GREATEST(best_streak, current_streak) WHERE name = ?", playerName);
        });
        return sql.queryInt("SELECT current_streak FROM chatgame_players WHERE name = ?", playerName);
    }

    public @NotNull @Unmodifiable Map<String, Integer> resetStreaks(@NotNull Collection<String> playerNames) {
        if (playerNames.isEmpty()) return Map.of();

        String names = placeholdersFor(playerNames);
        Object[] parameters = playerNames.toArray();

        Map<String, Integer> lostStreaks = sql.queryList(
                "SELECT name, current_streak FROM chatgame_players WHERE current_streak > 0 AND name IN (" + names + ")",
                row -> Map.entry(row.getString("name"), row.getInt("current_streak")),
                parameters
        ).stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!lostStreaks.isEmpty()) {
            sql.update("UPDATE chatgame_players SET current_streak = 0 WHERE name IN (" + names + ")", parameters);
        }
        return lostStreaks;
    }

    public @NotNull @Unmodifiable List<PlayerStats> findByNames(@NotNull Collection<String> playerNames) {
        if (playerNames.isEmpty()) return List.of();
        return sql.queryList("SELECT * FROM chatgame_players WHERE name IN (" + placeholdersFor(playerNames) + ")",
                PlayerStatsRepository::mapStats, playerNames.toArray());
    }

    public @NotNull @Unmodifiable List<PlayerStats> findMostWins(int limit) {
        return sql.queryList("SELECT * FROM chatgame_players WHERE wins > 0 ORDER BY wins DESC, name LIMIT ?",
                PlayerStatsRepository::mapStats, limit);
    }

    public @NotNull @Unmodifiable List<PlayerStats> findFastest(int limit) {
        return sql.queryList("SELECT * FROM chatgame_players WHERE fastest_time < ? ORDER BY fastest_time, name LIMIT ?",
                PlayerStatsRepository::mapStats, PlayerStats.NO_TIME, limit);
    }

    private static @NotNull String placeholdersFor(@NotNull Collection<?> values) {
        return String.join(", ", Collections.nCopies(values.size(), "?"));
    }

    private static @NotNull PlayerStats mapStats(@NotNull ResultSet row) throws SQLException {
        return new PlayerStats(
                row.getString("name"),
                row.getInt("wins"),
                row.getDouble("fastest_time"),
                row.getInt("current_streak"),
                row.getInt("best_streak")
        );
    }
}
