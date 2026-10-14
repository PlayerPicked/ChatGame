package com.mongenscave.mcchatgame.proxy;

import com.google.gson.Gson;
import com.mongenscave.mcchatgame.McChatGame;
import com.mongenscave.mcchatgame.identifiers.GameType;
import com.mongenscave.mcchatgame.proxy.messages.GameMessageType;
import com.mongenscave.mcchatgame.utils.LoggerUtils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

/**
 * SIMPLE Redis publisher - master validates and broadcasts result
 */
public class RedisPublisher {
    private final RedisConfig redisConfig;
    private final Gson gson;
    @Getter private final String serverId;
    private final String channelPrefix;

    public RedisPublisher(@NotNull RedisConfig redisConfig, @NotNull String serverId) {
        this.redisConfig = redisConfig;
        this.serverId = serverId;
        this.gson = new Gson();
        this.channelPrefix = McChatGame.getInstance().getConfiguration()
                .getString("redis.channel-prefix", "mcchatgame");
    }

    public void publishGameStart(@NotNull GameType gameType, @NotNull String gameData, long startTime) {
        GameMessage message = new GameMessage(gameType, serverId, GameMessageType.GAME_START)
                .addPayload("gameData", gameData)
                .addPayload("startTime", startTime);

        publish(message);
        LoggerUtils.info("Published GAME_START: {} - Data: {}", gameType, gameData);
    }

    public void publishGameStop(@NotNull GameType gameType) {
        GameMessage message = new GameMessage(gameType, serverId, GameMessageType.GAME_STOP);
        publish(message);
        LoggerUtils.info("Published GAME_STOP: {}", gameType);
    }

    public void publishGameTimeout(@NotNull GameType gameType, @NotNull String correctAnswer) {
        GameMessage message = new GameMessage(gameType, serverId, GameMessageType.GAME_TIMEOUT)
                .addPayload("correctAnswer", correctAnswer);

        publish(message);
        LoggerUtils.info("Published GAME_TIMEOUT: {} - Answer: {}", gameType, correctAnswer);
    }

    /**
     * SIMPLE: Master broadcasts that an answer was correct
     * All servers (including slaves) will process this locally for their own players
     */
    public void publishCorrectAnswer(@NotNull String playerName, @NotNull GameType gameType, @NotNull String answer) {
        GameMessage message = new GameMessage(gameType, serverId, GameMessageType.ANSWER_CORRECT)
                .addPayload("playerName", playerName)
                .addPayload("answer", answer);

        publish(message);
        LoggerUtils.info("Published ANSWER_CORRECT: {} answered '{}' correctly for {}", playerName, answer, gameType);
    }

    /**
     * Slave sends answer to master for validation
     */
    public void publishPlayerAnswer(@NotNull String playerName, @NotNull String answer, @NotNull GameType gameType) {
        GameMessage message = new GameMessage(gameType, serverId, GameMessageType.PLAYER_ANSWER)
                .addPayload("playerName", playerName)
                .addPayload("answer", answer);

        publish(message);
        LoggerUtils.info("Published PLAYER_ANSWER: {} answered '{}' for {}", playerName, answer, gameType);
    }

    private void publish(@NotNull GameMessage message) {
        if (!redisConfig.isConnected()) {
            LoggerUtils.warn("Cannot publish message - Redis not connected");
            return;
        }

        try (Jedis jedis = redisConfig.getJedisPool().getResource()) {
            String channel = channelPrefix + ":game";
            String json = gson.toJson(message.toJson());

            jedis.publish(channel, json);

            LoggerUtils.info("✓ Published {} for {} to Redis", message.getMessageType(), message.getGameType());
        } catch (Exception exception) {
            LoggerUtils.error("Failed to publish message: {}", exception.getMessage());
            exception.printStackTrace();
        }
    }
}