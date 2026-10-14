package com.playerpicked.chatgame.storage;

import org.jetbrains.annotations.NotNull;

public final class DataAccessException extends RuntimeException {
    public DataAccessException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }
}
