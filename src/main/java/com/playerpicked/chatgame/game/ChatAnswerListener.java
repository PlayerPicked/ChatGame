package com.playerpicked.chatgame.game;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public final class ChatAnswerListener implements Listener {
    private static final String ANSWER_PREFIX = "!";

    private final GameService games;

    public ChatAnswerListener(@NotNull GameService games) {
        this.games = games;
    }

    @EventHandler
    public void onChat(@NotNull AsyncChatEvent event) {
        if (games.currentRound().isEmpty()) return;

        String message = PlainTextComponentSerializer.plainText().serialize(event.originalMessage()).trim();
        if (message.startsWith(ANSWER_PREFIX)) message = message.substring(ANSWER_PREFIX.length()).trim();
        if (message.isEmpty()) return;

        games.submit(event.getPlayer(), Answer.chat(message));
    }
}
