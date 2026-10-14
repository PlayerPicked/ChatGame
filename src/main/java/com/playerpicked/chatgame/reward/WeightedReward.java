package com.playerpicked.chatgame.reward;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record WeightedReward(int weight, @NotNull String command) {
    public static final int GUARANTEED_WEIGHT = 100;

    private static final Pattern WEIGHT_PREFIX = Pattern.compile("\\[(\\d{1,6})]\\s*(.+)");

    public static @NotNull @Unmodifiable List<WeightedReward> parseAll(@NotNull List<String> entries) {
        List<WeightedReward> rewards = new ArrayList<>();
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) continue;

            Matcher matcher = WEIGHT_PREFIX.matcher(entry.trim());
            if (!matcher.matches()) {
                rewards.add(new WeightedReward(1, entry.trim()));
                continue;
            }

            int weight = Integer.parseInt(matcher.group(1));
            if (weight > 0) rewards.add(new WeightedReward(weight, matcher.group(2).trim()));
        }
        return List.copyOf(rewards);
    }

    public static @NotNull @Unmodifiable List<WeightedReward> draw(@NotNull List<WeightedReward> rewards, @NotNull RandomGenerator random) {
        List<WeightedReward> drawn = new ArrayList<>(rewards.stream().filter(WeightedReward::isGuaranteed).toList());
        List<WeightedReward> lottery = rewards.stream().filter(reward -> !reward.isGuaranteed()).toList();

        int totalWeight = lottery.stream().mapToInt(WeightedReward::weight).sum();
        if (totalWeight <= 0) return List.copyOf(drawn);

        int roll = random.nextInt(totalWeight);
        for (WeightedReward reward : lottery) {
            roll -= reward.weight();
            if (roll < 0) {
                drawn.add(reward);
                break;
            }
        }
        return List.copyOf(drawn);
    }

    public boolean isGuaranteed() {
        return weight == GUARANTEED_WEIGHT;
    }
}
