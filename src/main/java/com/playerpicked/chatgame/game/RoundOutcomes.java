package com.playerpicked.chatgame.game;

import com.playerpicked.chatgame.config.PluginConfig;
import com.playerpicked.chatgame.notification.SoundPlayer;
import com.playerpicked.chatgame.notification.ToastSender;
import com.playerpicked.chatgame.reward.RewardService;
import com.playerpicked.chatgame.stats.StatsService;
import com.playerpicked.chatgame.streak.StreakService;
import com.playerpicked.chatgame.text.MessageKey;
import com.playerpicked.chatgame.text.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public final class RoundOutcomes {
    private final PluginConfig config;
    private final Messages messages;
    private final RoundAnnouncer announcer;
    private final StatsService stats;
    private final RewardService rewards;
    private final StreakService streaks;
    private final ToastSender toasts;
    private final SoundPlayer sounds;
    private final Logger logger;

    public RoundOutcomes(@NotNull PluginConfig config, @NotNull Messages messages, @NotNull RoundAnnouncer announcer,
                         @NotNull StatsService stats, @NotNull RewardService rewards, @NotNull StreakService streaks,
                         @NotNull ToastSender toasts, @NotNull SoundPlayer sounds, @NotNull Logger logger) {
        this.config = config;
        this.messages = messages;
        this.announcer = announcer;
        this.stats = stats;
        this.rewards = rewards;
        this.streaks = streaks;
        this.toasts = toasts;
        this.sounds = sounds;
        this.logger = logger;
    }

    public void won(@NotNull ActiveRound round, @NotNull Contestant winner, double seconds) {
        announcer.announceWin(round, winner.name(), seconds);

        Player localWinner = Bukkit.getPlayer(winner.uuid());
        if (localWinner != null) rewardLocalWinner(localWinner, seconds);

        streaks.recordRound(round.participantNames(), winner.name(), localWinner != null);
    }

    public void expired(@NotNull ActiveRound round) {
        announcer.announceExpiry(round);
        streaks.recordRound(round.participantNames(), null, false);
    }

    private void rewardLocalWinner(@NotNull Player winner, double seconds) {
        String winnerName = winner.getName();
        rewards.rewardWinner(winnerName);
        stats.recordWin(winnerName, seconds).exceptionally(error -> {
            logger.error("Could not store the win of {}", winnerName, error);
            return null;
        });

        if (config.toastEnabled()) toasts.show(winner, messages.single(MessageKey.TOAST_TITLE), config.toastIcon());
        sounds.playTo(winner, config.winSound());
    }
}
