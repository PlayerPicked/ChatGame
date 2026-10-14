package com.playerpicked.chatgame.network;

import com.playerpicked.chatgame.game.AnswerSource;
import com.playerpicked.chatgame.game.GameType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public sealed interface NetworkMessage {
    @NotNull String sourceServerId();

    @NotNull UUID roundId();

    record RoundStarted(@NotNull String sourceServerId, @NotNull UUID roundId, @NotNull GameType game,
                        @NotNull List<String> seed, long startedAtMillis, int durationSeconds)
            implements NetworkMessage {
    }

    record AnswerSubmitted(@NotNull String sourceServerId, @NotNull UUID roundId, @NotNull UUID playerUuid,
                           @NotNull String playerName, @NotNull String answer, @NotNull AnswerSource source)
            implements NetworkMessage {
    }

    record RoundProgressed(@NotNull String sourceServerId, @NotNull UUID roundId, @NotNull List<String> progress)
            implements NetworkMessage {
    }

    record RoundWon(@NotNull String sourceServerId, @NotNull UUID roundId, @NotNull UUID winnerUuid,
                    @NotNull String winnerName, double seconds)
            implements NetworkMessage {
    }

    record RoundExpired(@NotNull String sourceServerId, @NotNull UUID roundId)
            implements NetworkMessage {
    }

    record RoundCancelled(@NotNull String sourceServerId, @NotNull UUID roundId)
            implements NetworkMessage {
    }
}
