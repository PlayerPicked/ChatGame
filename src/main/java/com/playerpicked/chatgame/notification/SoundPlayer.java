package com.playerpicked.chatgame.notification;

import com.playerpicked.chatgame.scheduler.ServerScheduler;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Optional;

public final class SoundPlayer {
    private static final float VOLUME = 0.5f;
    private static final float PITCH = 1.0f;

    private final ServerScheduler scheduler;
    private final Logger logger;

    public SoundPlayer(@NotNull ServerScheduler scheduler, @NotNull Logger logger) {
        this.scheduler = scheduler;
        this.logger = logger;
    }

    public void playToEveryone(@NotNull Optional<String> soundKey) {
        soundKey.flatMap(this::parse).ifPresent(sound -> scheduler.runGlobal(() ->
                Bukkit.getOnlinePlayers().forEach(player -> scheduler.runForPlayer(player, () -> player.playSound(sound)))));
    }

    public void playTo(@NotNull Player player, @NotNull Optional<String> soundKey) {
        soundKey.flatMap(this::parse).ifPresent(sound -> scheduler.runForPlayer(player, () -> player.playSound(sound)));
    }

    private @NotNull Optional<Sound> parse(@NotNull String soundKey) {
        @Subst("minecraft:entity.player.levelup") String key = soundKey;
        try {
            return Optional.of(Sound.sound(Key.key(key), Sound.Source.MASTER, VOLUME, PITCH));
        } catch (InvalidKeyException exception) {
            logger.warn("Invalid sound key in config.yml: {}", soundKey);
            return Optional.empty();
        }
    }
}
