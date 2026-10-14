package com.playerpicked.chatgame.game;

import com.playerpicked.chatgame.config.ConfigNames;
import org.jetbrains.annotations.NotNull;

public enum GameType {
    MATH,
    WHO_AM_I,
    WORD_GUESSER,
    RANDOM_CHARACTERS,
    WORD_STOP,
    FILL_OUT,
    REVERSE,
    CRAFTING,
    HANGMAN,
    RANGE;

    public @NotNull String configSection() {
        return ConfigNames.of(this);
    }

    public @NotNull String messageRoute(@NotNull String message) {
        return "games." + configSection() + "." + message;
    }
}
