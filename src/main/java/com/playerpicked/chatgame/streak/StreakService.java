package com.playerpicked.chatgame.streak;

import com.playerpicked.chatgame.config.PluginConfig;
import com.playerpicked.chatgame.config.StreakMilestone;
import com.playerpicked.chatgame.notification.ChatBroadcaster;
import com.playerpicked.chatgame.reward.RewardService;
import com.playerpicked.chatgame.scheduler.AsyncExecutor;
import com.playerpicked.chatgame.stats.StatsCache;
import com.playerpicked.chatgame.storage.PlayerStatsRepository;
import com.playerpicked.chatgame.text.MessageKey;
import com.playerpicked.chatgame.text.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class StreakService {
    private final PluginConfig config;
    private final PlayerStatsRepository repository;
    private final StatsCache statsCache;
    private final RewardService rewards;
    private final Messages messages;
    private final ChatBroadcaster broadcaster;
    private final AsyncExecutor async;
    private final Logger logger;

    public StreakService(@NotNull PluginConfig config, @NotNull PlayerStatsRepository repository, @NotNull StatsCache statsCache,
                         @NotNull RewardService rewards, @NotNull Messages messages, @NotNull ChatBroadcaster broadcaster,
                         @NotNull AsyncExecutor async, @NotNull Logger logger) {
        this.config = config;
        this.repository = repository;
        this.statsCache = statsCache;
        this.rewards = rewards;
        this.messages = messages;
        this.broadcaster = broadcaster;
        this.async = async;
        this.logger = logger;
    }

    public void recordRound(@NotNull Set<String> participantNames, @Nullable String winnerName, boolean winnerIsLocal) {
        if (!config.streaksEnabled()) return;

        List<String> losers = participantNames.stream()
                .filter(name -> !name.equals(winnerName))
                .filter(name -> Bukkit.getPlayerExact(name) != null)
                .toList();
        List<StreakMilestone> milestones = config.streakMilestones();

        async.run(() -> {
            if (winnerName != null && winnerIsLocal) advanceStreak(winnerName, milestones);
            repository.resetStreaks(losers).forEach((name, lostStreak) -> announceLoss(name, lostStreak, milestones));
            losers.forEach(statsCache::refreshPlayer);
        }).exceptionally(error -> {
            logger.error("Could not update win streaks", error);
            return null;
        });
    }

    private void advanceStreak(@NotNull String winnerName, @NotNull List<StreakMilestone> milestones) {
        int streak = repository.incrementStreak(winnerName);
        statsCache.refreshPlayer(winnerName);

        milestones.stream().filter(milestone -> milestone.streak() == streak).findFirst().ifPresent(milestone -> {
            if (milestone.announceReached()) {
                broadcaster.broadcast(messages.lines(MessageKey.STREAK_REACHED, Placeholder.unparsed("player", winnerName),
                        Placeholder.unparsed("streak", String.valueOf(streak))));
            }
            rewards.runAsConsole(milestone.commands(), Map.of("{player}", winnerName, "{streak}", String.valueOf(streak)));
        });
    }

    private void announceLoss(@NotNull String playerName, int lostStreak, @NotNull List<StreakMilestone> milestones) {
        Optional<StreakMilestone> highestReached = milestones.stream()
                .filter(milestone -> milestone.streak() <= lostStreak)
                .reduce((lower, higher) -> higher);
        if (highestReached.isEmpty() || !highestReached.get().announceLost()) return;

        broadcaster.broadcast(messages.lines(MessageKey.STREAK_LOST, Placeholder.unparsed("player", playerName),
                Placeholder.unparsed("streak", String.valueOf(lostStreak))));
    }
}
