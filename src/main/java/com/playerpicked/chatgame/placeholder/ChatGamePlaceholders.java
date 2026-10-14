package com.playerpicked.chatgame.placeholder;

import com.playerpicked.chatgame.game.ActiveRound;
import com.playerpicked.chatgame.game.GameService;
import com.playerpicked.chatgame.game.RoundAnnouncer;
import com.playerpicked.chatgame.stats.PlayerStats;
import com.playerpicked.chatgame.stats.StatsCache;
import com.playerpicked.chatgame.text.MessageKey;
import com.playerpicked.chatgame.text.Messages;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public final class ChatGamePlaceholders extends PlaceholderExpansion {
    private static final String MISSING = "---";
    private static final String NO_FASTEST_TIME = "00.00";

    private final String version;
    private final GameService games;
    private final StatsCache stats;
    private final Messages messages;

    public ChatGamePlaceholders(@NotNull String version, @NotNull GameService games, @NotNull StatsCache stats,
                                @NotNull Messages messages) {
        this.version = version;
        this.games = games;
        this.stats = stats;
        this.messages = messages;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "chatgame";
    }

    @Override
    public @NotNull String getAuthor() {
        return "coma112";
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer offlinePlayer, @NotNull String params) {
        if (params.startsWith("fastest_time_player_")) return leaderboard(params, "fastest_time_player_", stats::fastestAt, PlayerStats::name);
        if (params.startsWith("fastest_time_")) return leaderboard(params, "fastest_time_", stats::fastestAt, entry -> RoundAnnouncer.formatSeconds(entry.fastestSeconds()));
        if (params.startsWith("most_win_player_")) return leaderboard(params, "most_win_player_", stats::mostWinsAt, PlayerStats::name);
        if (params.startsWith("most_win_")) return leaderboard(params, "most_win_", stats::mostWinsAt, entry -> String.valueOf(entry.wins()));
        if (params.equals("current_game")) return currentGame();

        if (!(offlinePlayer instanceof Player player)) return null;
        PlayerStats playerStats = stats.of(player);

        return switch (params) {
            case "fastest_time" -> playerStats.hasFastestTime() ? RoundAnnouncer.formatSeconds(playerStats.fastestSeconds()) : NO_FASTEST_TIME;
            case "wins" -> String.valueOf(playerStats.wins());
            case "streak" -> String.valueOf(playerStats.currentStreak());
            case "best_streak" -> String.valueOf(playerStats.bestStreak());
            default -> null;
        };
    }

    private @NotNull String currentGame() {
        Optional<ActiveRound> current = games.currentRound();
        if (current.isEmpty()) return legacy(messages.single(MessageKey.PLACEHOLDER_NO_GAME));

        ActiveRound round = current.get();
        return legacy(messages.single("placeholders." + round.type().configSection(),
                Placeholder.unparsed("puzzle", round.puzzle().hint()),
                Placeholder.unparsed("time", String.valueOf(round.secondsRemaining()))));
    }

    private static @NotNull String leaderboard(@NotNull String params, @NotNull String prefix,
                                               @NotNull Function<Integer, Optional<PlayerStats>> lookup,
                                               @NotNull Function<PlayerStats, String> value) {
        try {
            int position = Integer.parseInt(params.substring(prefix.length()));
            return lookup.apply(position).map(value).orElse(MISSING);
        } catch (NumberFormatException ignored) {
            return "";
        }
    }

    private static @NotNull String legacy(@NotNull Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
