package com.playerpicked.chatgame.text;

import com.playerpicked.chatgame.game.GameType;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundledMessagesTest {
    private static final List<String> PLACEHOLDERS = List.of("prefix", "player", "time", "answer", "puzzle", "stages", "min", "max",
            "game", "item", "letter", "line", "streak", "current", "latest");

    @Test
    void everyMessageParsesWithoutUnknownTags() throws IOException {
        YamlDocument messages = load("messages.yml");
        MiniMessage miniMessage = MiniMessage.miniMessage();
        TagResolver placeholders = TagResolver.resolver(PLACEHOLDERS.stream().map(name -> Placeholder.unparsed(name, "x")).toList());

        List<String> checked = new ArrayList<>();
        for (String route : messages.getRoutesAsStrings(true)) {
            List<String> lines = messages.isList(route) ? messages.getStringList(route)
                    : messages.isString(route) ? List.of(messages.getString(route)) : List.of();

            for (String line : lines) {
                if (route.equals("games.hangman.stages")) continue;
                String plain = PlainTextComponentSerializer.plainText().serialize(miniMessage.deserialize(line, placeholders));
                assertFalse(plain.contains("<"), () -> route + " has an unknown tag: " + plain);
                checked.add(route);
            }
        }
        assertTrue(checked.size() > 30);
    }

    @Test
    void everyGameHasItsMessagesAndPlaceholderFormat() throws IOException {
        YamlDocument messages = load("messages.yml");
        for (GameType type : GameType.values()) {
            for (String message : List.of("announce", "won", "expired")) {
                assertTrue(messages.contains(type.messageRoute(message)), type.messageRoute(message));
            }
            assertTrue(messages.contains("placeholders." + type.configSection()), type.configSection());
        }
    }

    private static YamlDocument load(String resource) throws IOException {
        try (InputStream stream = Objects.requireNonNull(BundledMessagesTest.class.getClassLoader().getResourceAsStream(resource))) {
            return YamlDocument.create(stream);
        }
    }
}
