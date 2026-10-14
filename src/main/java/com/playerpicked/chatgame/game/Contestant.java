package com.playerpicked.chatgame.game;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record Contestant(@NotNull UUID uuid, @NotNull String name) {
    public static @NotNull Contestant of(@NotNull Player player) {
        return new Contestant(player.getUniqueId(), player.getName());
    }
}
