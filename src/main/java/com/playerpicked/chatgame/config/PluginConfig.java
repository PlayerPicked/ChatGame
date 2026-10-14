package com.playerpicked.chatgame.config;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class PluginConfig {
    private static final List<String> DEFAULT_COMMAND_ALIASES = List.of("chatgame");

    private final YamlFile file;
    private final String generatedServerId = UUID.randomUUID().toString();

    public PluginConfig(@NotNull YamlFile file) {
        this.file = file;
    }

    public void reload() {
        file.reload();
    }

    public @NotNull List<String> commandAliases() {
        List<String> aliases = file.getStringList("aliases");
        return aliases.isEmpty() ? DEFAULT_COMMAND_ALIASES : aliases;
    }

    public int secondsBetweenRounds() {
        return Math.max(0, file.getInt("time-between-games", 20));
    }

    public int minPlayers() {
        return Math.max(0, file.getInt("min-players", 1));
    }

    public @NotNull @Unmodifiable List<String> rewards() {
        return List.copyOf(file.getStringList("rewards"));
    }

    public @NotNull GameSettings game(@NotNull String sectionName) {
        return new GameSettings(file, sectionName);
    }

    public boolean streaksEnabled() {
        return file.getBoolean("streaks.enabled", true);
    }

    public @NotNull @Unmodifiable List<StreakMilestone> streakMilestones() {
        Section milestones = file.getSection("streaks.milestones");
        if (milestones == null) return List.of();

        List<StreakMilestone> parsed = new ArrayList<>();
        for (Object key : milestones.getKeys()) {
            Section milestone = milestones.getSection(String.valueOf(key));
            Optional<Integer> streak = parseInt(String.valueOf(key));
            if (milestone == null || streak.isEmpty()) continue;

            parsed.add(new StreakMilestone(
                    streak.get(),
                    milestone.getBoolean("announce-reached", true),
                    milestone.getBoolean("announce-lost", true),
                    List.copyOf(milestone.getStringList("commands"))
            ));
        }
        parsed.sort(Comparator.comparingInt(StreakMilestone::streak));
        return List.copyOf(parsed);
    }

    public boolean toastEnabled() {
        return file.getBoolean("toast.enabled", true);
    }

    public @NotNull Material toastIcon() {
        Material icon = Material.matchMaterial(file.getString("toast.material", "BOOK"));
        return icon != null && icon.isItem() ? icon : Material.BOOK;
    }

    public @NotNull Optional<String> startSound() {
        return sound("start");
    }

    public @NotNull Optional<String> winSound() {
        return sound("win");
    }

    public @NotNull DatabaseSettings database() {
        DatabaseType type = file.getString("database.type", "h2").toLowerCase(Locale.ROOT).equals("mysql")
                ? DatabaseType.MYSQL
                : DatabaseType.H2;

        return new DatabaseSettings(
                type,
                file.getString("database.mysql.host", "localhost"),
                file.getInt("database.mysql.port", 3306),
                file.getString("database.mysql.database", "chatgame"),
                file.getString("database.mysql.username", "root"),
                file.getString("database.mysql.password", ""),
                Math.max(2, file.getInt("database.mysql.pool-size", 10))
        );
    }

    public @NotNull RedisSettings redis() {
        String configuredServerId = file.getString("redis.server-id", "").trim();

        return new RedisSettings(
                file.getBoolean("redis.enabled", false),
                file.getString("redis.host", "localhost"),
                file.getInt("redis.port", 6379),
                file.getString("redis.password", ""),
                file.getInt("redis.database", 0),
                file.getBoolean("redis.ssl", false),
                Math.max(500, file.getInt("redis.timeout", 2000)),
                !file.getString("redis.server-role", "master").equalsIgnoreCase("slave"),
                configuredServerId.isEmpty() ? generatedServerId : configuredServerId,
                file.getString("redis.channel-prefix", "chatgame")
        );
    }

    private @NotNull Optional<String> sound(@NotNull String event) {
        if (!file.getBoolean("sounds." + event + ".enabled", true)) return Optional.empty();
        String key = file.getString("sounds." + event + ".sound", "").trim();
        return key.isEmpty() ? Optional.empty() : Optional.of(key);
    }

    private static @NotNull Optional<Integer> parseInt(@NotNull String text) {
        try {
            return Optional.of(Integer.parseInt(text.trim()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
