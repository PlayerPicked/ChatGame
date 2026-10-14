package com.playerpicked.chatgame.text;

import com.playerpicked.chatgame.config.YamlFile;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public final class Messages {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final YamlFile file;

    public Messages(@NotNull YamlFile file) {
        this.file = file;
    }

    public void reload() {
        file.reload();
    }

    public void send(@NotNull Audience audience, @NotNull MessageKey key, @NotNull TagResolver... placeholders) {
        lines(key.route(), placeholders).forEach(audience::sendMessage);
    }

    public @NotNull @Unmodifiable List<String> rawLines(@NotNull String route) {
        return file.getLines(route);
    }

    public @NotNull Component single(@NotNull MessageKey key, @NotNull TagResolver... placeholders) {
        return single(key.route(), placeholders);
    }

    public @NotNull @Unmodifiable List<Component> lines(@NotNull MessageKey key, @NotNull TagResolver... placeholders) {
        return lines(key.route(), placeholders);
    }

    public @NotNull @Unmodifiable List<Component> lines(@NotNull String route, @NotNull TagResolver... placeholders) {
        TagResolver resolver = withPrefix(placeholders);
        return file.getLines(route).stream()
                .map(line -> MINI_MESSAGE.deserialize(line, resolver))
                .toList();
    }

    public @NotNull Component single(@NotNull String route, @NotNull TagResolver... placeholders) {
        String text = file.getString(route, "");
        return text.isEmpty() ? Component.empty() : MINI_MESSAGE.deserialize(text, withPrefix(placeholders));
    }

    public boolean has(@NotNull String route) {
        return !file.getLines(route).isEmpty();
    }

    public static @NotNull Component render(@NotNull String miniMessage, @NotNull TagResolver placeholders) {
        return MINI_MESSAGE.deserialize(miniMessage, placeholders);
    }

    private @NotNull TagResolver withPrefix(@NotNull TagResolver... placeholders) {
        Component prefix = MINI_MESSAGE.deserialize(file.getString("prefix", ""));
        return TagResolver.resolver(TagResolver.resolver(placeholders), Placeholder.component("prefix", prefix));
    }
}
