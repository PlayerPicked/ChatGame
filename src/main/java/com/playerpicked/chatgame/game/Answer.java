package com.playerpicked.chatgame.game;

import org.jetbrains.annotations.NotNull;

public record Answer(@NotNull String text, @NotNull AnswerSource source) {
    public static @NotNull Answer chat(@NotNull String text) {
        return new Answer(text, AnswerSource.CHAT);
    }

    public boolean isFromChat() {
        return source == AnswerSource.CHAT;
    }
}
