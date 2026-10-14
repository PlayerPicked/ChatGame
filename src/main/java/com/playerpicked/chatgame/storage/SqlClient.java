package com.playerpicked.chatgame.storage;

import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqlClient {
    private final DataSource dataSource;

    public SqlClient(@NotNull DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(@NotNull ResultSet row) throws SQLException;
    }

    @FunctionalInterface
    public interface TransactionWork {
        void run(@NotNull Transaction transaction) throws SQLException;
    }

    public static final class Transaction {
        private final Connection connection;

        private Transaction(@NotNull Connection connection) {
            this.connection = connection;
        }

        public int update(@NotNull String sql, @NotNull Object... parameters) throws SQLException {
            try (PreparedStatement statement = prepare(connection, sql, parameters)) {
                return statement.executeUpdate();
            }
        }
    }

    public <T> @NotNull List<T> queryList(@NotNull String sql, @NotNull RowMapper<T> mapper, @NotNull Object... parameters) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = prepare(connection, sql, parameters);
             ResultSet rows = statement.executeQuery()) {

            List<T> results = new ArrayList<>();
            while (rows.next()) results.add(mapper.map(rows));
            return results;
        } catch (SQLException exception) {
            throw new DataAccessException("Query failed: " + sql, exception);
        }
    }

    public <T> @NotNull Optional<T> queryFirst(@NotNull String sql, @NotNull RowMapper<T> mapper, @NotNull Object... parameters) {
        return queryList(sql, mapper, parameters).stream().findFirst();
    }

    public boolean exists(@NotNull String sql, @NotNull Object... parameters) {
        return queryFirst(sql, row -> true, parameters).isPresent();
    }

    public int queryInt(@NotNull String sql, @NotNull Object... parameters) {
        return queryFirst(sql, row -> row.getInt(1), parameters).orElse(0);
    }

    public int update(@NotNull String sql, @NotNull Object... parameters) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = prepare(connection, sql, parameters)) {
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Update failed: " + sql, exception);
        }
    }

    public void inTransaction(@NotNull TransactionWork work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                work.run(new Transaction(connection));
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Transaction failed", exception);
        }
    }

    public static @NotNull UUID readUuid(@NotNull ResultSet row, @NotNull String column) throws SQLException {
        return UUID.fromString(row.getString(column));
    }

    public static @NotNull Instant readInstant(@NotNull ResultSet row, @NotNull String column) throws SQLException {
        Timestamp timestamp = row.getTimestamp(column);
        return timestamp != null ? timestamp.toInstant() : Instant.EPOCH;
    }

    private static @NotNull PreparedStatement prepare(@NotNull Connection connection, @NotNull String sql,
                                                      @NotNull Object... parameters) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int index = 0; index < parameters.length; index++) {
            statement.setObject(index + 1, toJdbcValue(parameters[index]));
        }
        return statement;
    }

    private static Object toJdbcValue(Object value) {
        return switch (value) {
            case UUID uuid -> uuid.toString();
            case Instant instant -> Timestamp.from(instant);
            case Enum<?> constant -> constant.name();
            case null, default -> value;
        };
    }
}
