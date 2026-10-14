package com.playerpicked.chatgame.update;

import com.playerpicked.chatgame.scheduler.ServerScheduler;
import com.playerpicked.chatgame.text.MessageKey;
import com.playerpicked.chatgame.text.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;

public final class UpdateChecker implements Listener {
    private static final Duration CHECK_INTERVAL = Duration.ofHours(6);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final long JOIN_NOTIFICATION_DELAY_TICKS = 50;
    private static final String PERMISSION = "chatgame.update";
    private static final String VERSION_FORMAT = "\\d+(\\.\\d+)*([-+][0-9A-Za-z.]+)?";

    private final int spigotResourceId;
    private final String currentVersion;
    private final Messages messages;
    private final ServerScheduler scheduler;
    private final Logger logger;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();

    private volatile String newerVersion;

    public UpdateChecker(int spigotResourceId, @NotNull String currentVersion, @NotNull Messages messages,
                         @NotNull ServerScheduler scheduler, @NotNull Logger logger) {
        this.spigotResourceId = spigotResourceId;
        this.currentVersion = currentVersion;
        this.messages = messages;
        this.scheduler = scheduler;
        this.logger = logger;
    }

    public void start() {
        if (spigotResourceId <= 0) return;
        scheduler.runGlobalTimer(1, CHECK_INTERVAL.toSeconds() * 20, this::check);
    }

    public void shutdown() {
        http.close();
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String latest = newerVersion;
        if (latest == null || !player.hasPermission(PERMISSION)) return;

        scheduler.runForPlayerLater(player, JOIN_NOTIFICATION_DELAY_TICKS, () -> messages.send(player, MessageKey.UPDATE_AVAILABLE, placeholders(latest)));
    }

    private void check() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.spigotmc.org/legacy/update.php?resource=" + spigotResourceId))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.body().trim())
                .thenAccept(latest -> {
                    if (!latest.matches(VERSION_FORMAT) || compareVersions(latest, currentVersion) <= 0) return;
                    if (latest.equals(newerVersion)) return;

                    newerVersion = latest;
                    logger.info("ChatGame {} is available (running {})", latest, currentVersion);
                })
                .exceptionally(error -> {
                    logger.debug("Could not check for updates: {}", error.getMessage());
                    return null;
                });
    }

    private @NotNull TagResolver placeholders(@NotNull String latest) {
        return TagResolver.resolver(Placeholder.unparsed("current", currentVersion), Placeholder.unparsed("latest", latest));
    }

    static int compareVersions(@NotNull String first, @NotNull String second) {
        int[] firstParts = parseVersion(first);
        int[] secondParts = parseVersion(second);
        for (int index = 0; index < Math.max(firstParts.length, secondParts.length); index++) {
            int difference = Integer.compare(partAt(firstParts, index), partAt(secondParts, index));
            if (difference != 0) return difference;
        }
        return 0;
    }

    private static int[] parseVersion(@NotNull String version) {
        return Arrays.stream(version.split("[.\\-]"))
                .map(part -> part.replaceAll("\\D", ""))
                .filter(part -> !part.isEmpty() && part.length() < 9)
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    private static int partAt(int[] parts, int index) {
        return index < parts.length ? parts[index] : 0;
    }
}
