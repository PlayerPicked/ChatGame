package com.playerpicked.chatgame.reward;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedRewardTest {

    @Test
    void parsesWeightsAndDefaultsToOne() {
        List<WeightedReward> rewards = WeightedReward.parseAll(List.of("[20] give {player} gold_ingot 1", "say hi", "  ", "[0] never",
                "[99999999999] too big"));

        assertEquals(List.of(
                new WeightedReward(20, "give {player} gold_ingot 1"),
                new WeightedReward(1, "say hi"),
                new WeightedReward(1, "[99999999999] too big")
        ), rewards);
    }

    @Test
    void guaranteedRewardsAlwaysRunAndExactlyOneOtherIsDrawn() {
        List<WeightedReward> rewards = WeightedReward.parseAll(List.of("[100] always", "[20] a", "[15] b"));
        Random random = new Random(7);

        for (int draw = 0; draw < 50; draw++) {
            List<WeightedReward> drawn = WeightedReward.draw(rewards, random);
            assertEquals(2, drawn.size());
            assertEquals("always", drawn.getFirst().command());
            assertTrue(List.of("a", "b").contains(drawn.get(1).command()));
        }
    }
}
