package com.playerpicked.chatgame.network;

import com.playerpicked.chatgame.game.AnswerSource;
import com.playerpicked.chatgame.game.GameType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkMessageCodecTest {
    private final NetworkMessageCodec codec = new NetworkMessageCodec();

    @Test
    void everyMessageTypeSurvivesARoundTrip() {
        UUID round = UUID.randomUUID();
        List<NetworkMessage> messages = List.of(
                new NetworkMessage.RoundStarted("lobby-1", round, GameType.HANGMAN, List.of("ALMA", "6", "false"), 1234L, 120),
                new NetworkMessage.AnswerSubmitted("lobby-2", round, UUID.randomUUID(), "Alice", "IRON_SHOVEL", AnswerSource.CRAFTING_TABLE),
                new NetworkMessage.RoundProgressed("lobby-1", round, List.of("AZ", "1")),
                new NetworkMessage.RoundWon("lobby-1", round, UUID.randomUUID(), "Bob", 3.25),
                new NetworkMessage.RoundExpired("lobby-1", round),
                new NetworkMessage.RoundCancelled("lobby-1", round)
        );

        for (NetworkMessage message : messages) {
            assertEquals(message, codec.decode(codec.encode(message)).orElseThrow());
        }
    }

    @Test
    void malformedOrForeignMessagesAreIgnored() {
        assertTrue(codec.decode("not json").isEmpty());
        assertTrue(codec.decode("{\"type\":\"unknown\"}").isEmpty());
        assertTrue(codec.decode("{\"gameType\":\"MATH\",\"messageType\":\"GAME_START\"}").isEmpty());
        assertTrue(codec.decode("{\"type\":\"round-expired\"}").isEmpty());
    }
}
