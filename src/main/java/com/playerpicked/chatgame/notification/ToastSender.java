package com.playerpicked.chatgame.notification;

import com.google.gson.JsonObject;
import com.playerpicked.chatgame.scheduler.ServerScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ToastSender {
    private static final long TOAST_LIFETIME_TICKS = 100;

    private final Plugin plugin;
    private final ServerScheduler scheduler;
    private final Logger logger;
    private final Set<NamespacedKey> registeredToasts = ConcurrentHashMap.newKeySet();

    public ToastSender(@NotNull Plugin plugin, @NotNull ServerScheduler scheduler, @NotNull Logger logger) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.logger = logger;
    }

    public void show(@NotNull Player player, @NotNull Component title, @NotNull Material icon) {
        NamespacedKey key = new NamespacedKey(plugin, "toast_" + UUID.randomUUID());
        String advancementJson = createAdvancementJson(title, icon);

        scheduler.runGlobal(() -> {
            if (!register(key, advancementJson)) return;
            scheduler.runForPlayer(player, () -> grant(player, key));
            scheduler.runGlobalLater(TOAST_LIFETIME_TICKS, () -> unregister(key));
        });
    }

    public void shutdown() {
        registeredToasts.forEach(this::unregister);
    }

    // Paper has no toast API; a hidden, auto-granted advancement is the only way to show one
    @SuppressWarnings("deprecation")
    private boolean register(@NotNull NamespacedKey key, @NotNull String advancementJson) {
        try {
            Bukkit.getUnsafe().loadAdvancement(key, advancementJson);
            registeredToasts.add(key);
            return true;
        } catch (RuntimeException exception) {
            logger.warn("Could not create the win toast: {}", exception.getMessage());
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private void unregister(@NotNull NamespacedKey key) {
        if (!registeredToasts.remove(key)) return;
        Bukkit.getUnsafe().removeAdvancement(key);
    }

    private static void grant(@NotNull Player player, @NotNull NamespacedKey key) {
        Advancement advancement = Bukkit.getAdvancement(key);
        if (advancement == null) return;

        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        progress.getRemainingCriteria().forEach(progress::awardCriteria);
    }

    private static @NotNull String createAdvancementJson(@NotNull Component title, @NotNull Material icon) {
        JsonObject iconJson = new JsonObject();
        iconJson.addProperty("id", icon.getKey().toString());

        JsonObject display = new JsonObject();
        display.add("icon", iconJson);
        display.add("title", GsonComponentSerializer.gson().serializeToTree(title));
        display.add("description", GsonComponentSerializer.gson().serializeToTree(Component.empty()));
        display.addProperty("background", "minecraft:textures/gui/advancements/backgrounds/adventure.png");
        display.addProperty("frame", "task");
        display.addProperty("announce_to_chat", false);
        display.addProperty("show_toast", true);
        display.addProperty("hidden", true);

        JsonObject impossible = new JsonObject();
        impossible.addProperty("trigger", "minecraft:impossible");
        JsonObject criteria = new JsonObject();
        criteria.add("trigger", impossible);

        JsonObject advancement = new JsonObject();
        advancement.add("display", display);
        advancement.add("criteria", criteria);
        return advancement.toString();
    }
}
