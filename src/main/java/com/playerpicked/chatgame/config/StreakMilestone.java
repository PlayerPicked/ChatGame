package com.playerpicked.chatgame.config;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record StreakMilestone(int streak, boolean announceReached, boolean announceLost, @NotNull List<String> commands) {
}
