package com.playerpicked.chatgame.notification;

import com.playerpicked.chatgame.scheduler.ServerScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ChatBroadcaster {
    private final ServerScheduler scheduler;

    public ChatBroadcaster(@NotNull ServerScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void broadcast(@NotNull List<Component> lines) {
        if (lines.isEmpty()) return;
        scheduler.runGlobal(() -> Bukkit.getOnlinePlayers().forEach(player -> lines.forEach(player::sendMessage)));
    }
}
