package com.playerpicked.chatgame.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public final class NetworkMessageCodec {
    private static final String TYPE_FIELD = "type";

    private static final Map<String, Class<? extends NetworkMessage>> TYPES = Map.of(
            "round-started", NetworkMessage.RoundStarted.class,
            "answer-submitted", NetworkMessage.AnswerSubmitted.class,
            "round-progressed", NetworkMessage.RoundProgressed.class,
            "round-won", NetworkMessage.RoundWon.class,
            "round-expired", NetworkMessage.RoundExpired.class,
            "round-cancelled", NetworkMessage.RoundCancelled.class
    );

    private final Gson gson = new Gson();

    public @NotNull String encode(@NotNull NetworkMessage message) {
        JsonObject json = gson.toJsonTree(message).getAsJsonObject();
        if (json.has(TYPE_FIELD)) throw new IllegalStateException(message.getClass().getSimpleName() + " must not have a field named " + TYPE_FIELD);
        json.addProperty(TYPE_FIELD, typeIdOf(message));
        return gson.toJson(json);
    }

    public @NotNull Optional<NetworkMessage> decode(@NotNull String rawJson) {
        try {
            JsonObject json = JsonParser.parseString(rawJson).getAsJsonObject();
            Class<? extends NetworkMessage> type = TYPES.get(json.get(TYPE_FIELD).getAsString());
            return Optional.ofNullable(type).<NetworkMessage>map(messageType -> gson.fromJson(json, messageType))
                    .filter(NetworkMessageCodec::hasRequiredFields);
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException | NullPointerException exception) {
            return Optional.empty();
        }
    }

    private static boolean hasRequiredFields(@NotNull NetworkMessage message) {
        return message.sourceServerId() != null && message.roundId() != null;
    }

    private static @NotNull String typeIdOf(@NotNull NetworkMessage message) {
        return switch (message) {
            case NetworkMessage.RoundStarted ignored -> "round-started";
            case NetworkMessage.AnswerSubmitted ignored -> "answer-submitted";
            case NetworkMessage.RoundProgressed ignored -> "round-progressed";
            case NetworkMessage.RoundWon ignored -> "round-won";
            case NetworkMessage.RoundExpired ignored -> "round-expired";
            case NetworkMessage.RoundCancelled ignored -> "round-cancelled";
        };
    }
}
