package com.mongenscave.mcchatgame.proxy;

import com.google.gson.JsonObject;
import com.mongenscave.mcchatgame.identifiers.GameType;
import com.mongenscave.mcchatgame.proxy.messages.GameMessageType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified message format for all game-related Redis communication.
 * Simple approach: Master validates, broadcasts result, all servers handle locally
 */
@Getter
public class GameMessage {
    private final GameType gameType;
    private final String serverId;
    private final long timestamp;
    private final GameMessageType messageType;
    private final Map<String, Object> payload;

    public GameMessage(@NotNull GameType gameType, @NotNull String serverId, @NotNull GameMessageType messageType) {
        this.gameType = gameType;
        this.serverId = serverId;
        this.messageType = messageType;
        this.timestamp = System.currentTimeMillis();
        this.payload = new HashMap<>();
    }

    public GameMessage addPayload(@NotNull String key, @NotNull Object value) {
        payload.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getPayload(@NotNull String key, @NotNull Class<T> type) {
        return (T) payload.get(key);
    }

    @NotNull
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("gameType", gameType.name());
        json.addProperty("serverId", serverId);
        json.addProperty("messageType", messageType.name());
        json.addProperty("timestamp", timestamp);

        JsonObject payloadJson = new JsonObject();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                payloadJson.addProperty(key, (String) value);
            } else if (value instanceof Number) {
                payloadJson.addProperty(key, (Number) value);
            } else if (value instanceof Boolean) {
                payloadJson.addProperty(key, (Boolean) value);
            } else {
                payloadJson.addProperty(key, value.toString());
            }
        }
        json.add("payload", payloadJson);

        return json;
    }

    @NotNull
    public static GameMessage fromJson(@NotNull JsonObject json) {
        GameType gameType = GameType.valueOf(json.get("gameType").getAsString());
        String serverId = json.get("serverId").getAsString();
        GameMessageType messageType = GameMessageType.valueOf(json.get("messageType").getAsString());

        GameMessage message = new GameMessage(gameType, serverId, messageType);

        if (json.has("payload")) {
            JsonObject payloadJson = json.getAsJsonObject("payload");
            for (String key : payloadJson.keySet()) {
                if (payloadJson.get(key).isJsonPrimitive()) {
                    if (payloadJson.get(key).getAsJsonPrimitive().isString()) {
                        message.payload.put(key, payloadJson.get(key).getAsString());
                    } else if (payloadJson.get(key).getAsJsonPrimitive().isNumber()) {
                        message.payload.put(key, payloadJson.get(key).getAsDouble());
                    } else if (payloadJson.get(key).getAsJsonPrimitive().isBoolean()) {
                        message.payload.put(key, payloadJson.get(key).getAsBoolean());
                    }
                }
            }
        }

        return message;
    }
}