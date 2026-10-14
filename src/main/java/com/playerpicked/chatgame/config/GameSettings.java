package com.playerpicked.chatgame.config;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public final class GameSettings {
    private final YamlFile file;
    private final String section;

    GameSettings(@NotNull YamlFile file, @NotNull String section) {
        this.file = file;
        this.section = section;
    }

    public boolean isEnabled() {
        return file.getBoolean(route("enabled"), false);
    }

    public int roundSeconds() {
        return Math.max(1, file.getInt(route("time"), 60));
    }

    public @NotNull @Unmodifiable List<String> entries(@NotNull String listKey) {
        return file.getStringList(route(listKey)).stream()
                .filter(entry -> entry != null && !entry.isBlank())
                .map(String::trim)
                .toList();
    }

    public int getInt(@NotNull String key, int fallback) {
        return file.getInt(route(key), fallback);
    }

    public boolean getBoolean(@NotNull String key, boolean fallback) {
        return file.getBoolean(route(key), fallback);
    }

    public @NotNull String getString(@NotNull String key, @NotNull String fallback) {
        return file.getString(route(key), fallback);
    }

    public @Nullable Section getSection(@NotNull String key) {
        return file.getSection(route(key));
    }

    private @NotNull String route(@NotNull String key) {
        return section + "." + key;
    }
}
