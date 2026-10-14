package com.playerpicked.chatgame.reward;

import com.playerpicked.chatgame.config.PluginConfig;
import com.playerpicked.chatgame.scheduler.ServerScheduler;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class RewardService {
    private final PluginConfig config;
    private final ServerScheduler scheduler;

    public RewardService(@NotNull PluginConfig config, @NotNull ServerScheduler scheduler) {
        this.config = config;
        this.scheduler = scheduler;
    }

    public void rewardWinner(@NotNull String playerName) {
        List<WeightedReward> rewards = WeightedReward.parseAll(config.rewards());
        List<String> commands = WeightedReward.draw(rewards, ThreadLocalRandom.current()).stream()
                .map(WeightedReward::command)
                .toList();
        runAsConsole(commands, Map.of("{player}", playerName));
    }

    public void runAsConsole(@NotNull List<String> commands, @NotNull Map<String, String> replacements) {
        List<String> resolved = commands.stream().map(command -> replaceAll(command, replacements)).toList();
        if (resolved.isEmpty()) return;

        scheduler.runGlobal(() -> resolved.forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)));
    }

    private static @NotNull String replaceAll(@NotNull String command, @NotNull Map<String, String> replacements) {
        String resolved = command;
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            resolved = resolved.replace(replacement.getKey(), replacement.getValue());
        }
        return resolved;
    }
}
