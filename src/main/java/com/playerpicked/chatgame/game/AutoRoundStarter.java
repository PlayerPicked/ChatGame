package com.playerpicked.chatgame.game;

import com.playerpicked.chatgame.config.PluginConfig;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class AutoRoundStarter implements Runnable {
    public static final long CHECK_INTERVAL_TICKS = 20;

    private final PluginConfig config;
    private final GameService games;
    private final RoundTracker rounds;

    public AutoRoundStarter(@NotNull PluginConfig config, @NotNull GameService games, @NotNull RoundTracker rounds) {
        this.config = config;
        this.games = games;
        this.rounds = rounds;
    }

    @Override
    public void run() {
        if (!games.isAuthority() || rounds.current().isPresent()) return;
        if (Bukkit.getOnlinePlayers().size() < config.minPlayers()) return;

        long cooldownMillis = TimeUnit.SECONDS.toMillis(config.secondsBetweenRounds());
        if (System.currentTimeMillis() - rounds.lastEndedAtMillis() < cooldownMillis) return;

        startRandomEnabledGame();
    }

    private void startRandomEnabledGame() {
        List<GameType> enabled = new ArrayList<>(Arrays.stream(GameType.values())
                .filter(type -> config.game(type.configSection()).isEnabled())
                .toList());
        Collections.shuffle(enabled);

        for (GameType type : enabled) {
            if (games.start(type) == StartResult.STARTED) return;
        }
    }
}
