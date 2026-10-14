package com.playerpicked.chatgame.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

public final class YamlFile {
    private final YamlDocument document;

    private YamlFile(@NotNull YamlDocument document) {
        this.document = document;
    }

    public static @NotNull YamlFile loadFromPluginFolder(@NotNull Plugin plugin, @NotNull String fileName) {
        InputStream bundledDefaults = Objects.requireNonNull(plugin.getResource(fileName), "Missing bundled resource: " + fileName);

        try {
            YamlDocument document = YamlDocument.create(
                    new File(plugin.getDataFolder(), fileName),
                    bundledDefaults,
                    GeneralSettings.builder().setUseDefaults(false).build(),
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setKeepAll(true).setVersioning(new BasicVersioning("version")).build()
            );
            return new YamlFile(document);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not load " + fileName, exception);
        }
    }

    public void reload() {
        try {
            document.reload();
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not reload " + document.getFile(), exception);
        }
    }

    public @NotNull String getString(@NotNull String route, @NotNull String fallback) {
        return document.getString(route, fallback);
    }

    public int getInt(@NotNull String route, int fallback) {
        return document.getInt(route, fallback);
    }

    public float getFloat(@NotNull String route, float fallback) {
        return document.getFloat(route, fallback);
    }

    public boolean getBoolean(@NotNull String route, boolean fallback) {
        return document.getBoolean(route, fallback);
    }

    public @NotNull List<String> getStringList(@NotNull String route) {
        return document.getStringList(route);
    }

    public @NotNull List<String> getLines(@NotNull String route) {
        if (document.isList(route)) return document.getStringList(route);
        String singleLine = document.getString(route);
        return singleLine == null ? List.of() : List.of(singleLine);
    }

    public @Nullable Section getSection(@NotNull String route) {
        return document.getSection(route);
    }
}
