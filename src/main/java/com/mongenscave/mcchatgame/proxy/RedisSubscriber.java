package com.mongenscave.mcchatgame.proxy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mongenscave.mcchatgame.McChatGame;
import com.mongenscave.mcchatgame.managers.ProxyManager;
import com.mongenscave.mcchatgame.utils.LoggerUtils;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SIMPLE Redis subscriber - receives correct answer broadcasts and processes locally
 */
public class RedisSubscriber extends JedisPubSub {
    private final RedisConfig redisConfig;
    private final ProxyManager proxyManager;
    private final Gson gson;
    private final String serverId;
    private final String channelPrefix;
    private final ExecutorService executorService;
    private Thread subscriberThread;

    public RedisSubscriber(@NotNull RedisConfig redisConfig, @NotNull ProxyManager proxyManager, @NotNull String serverId) {
        this.redisConfig = redisConfig;
        this.proxyManager = proxyManager;
        this.serverId = serverId;
        this.gson = new Gson();
        this.channelPrefix = McChatGame.getInstance().getConfiguration()
                .getString("redis.channel-prefix", "mcchatgame");
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void subscribe() {
        if (subscriberThread != null && subscriberThread.isAlive()) {
            LoggerUtils.warn("Subscriber already running");
            return;
        }

        subscriberThread = Thread.ofVirtual().start(() -> {
            Jedis jedis = null;
            try {
                jedis = redisConfig.getJedisPool().getResource();

                String channel = channelPrefix + ":game";

                LoggerUtils.info("=== REDIS SUBSCRIBER STARTING ===");
                LoggerUtils.info("Server ID: {}", serverId);
                LoggerUtils.info("Server Role: {}", proxyManager.isMasterServer() ? "MASTER" : "SLAVE");
                LoggerUtils.info("Subscribing to: {}", channel);
                LoggerUtils.info("=================================");

                jedis.subscribe(this, channel);
            } catch (Exception exception) {
                LoggerUtils.error("Redis subscriber error: {}", exception.getMessage());
                exception.printStackTrace();
            } finally {
                if (jedis != null) {
                    try {
                        jedis.close();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    public void shutdownSubscriber() {
        try {
            if (isSubscribed()) {
                LoggerUtils.info("Unsubscribing from Redis channels...");
                super.unsubscribe();
            }

            if (subscriberThread != null && subscriberThread.isAlive()) {
                subscriberThread.interrupt();
                subscriberThread.join(5000);
            }

            executorService.shutdown();
            LoggerUtils.info("Redis subscriber stopped");
        } catch (Exception exception) {
            LoggerUtils.error("Error shutting down subscriber: {}", exception.getMessage());
        }
    }

    @Override
    public void onMessage(String channel, String message) {
        executorService.submit(() -> handleMessage(message));
    }

    private void handleMessage(@NotNull String message) {
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            GameMessage gameMessage = GameMessage.fromJson(json);

            // Ignore our own messages
            if (gameMessage.getServerId().equals(serverId)) {
                LoggerUtils.info("Ignoring own message from server {}", serverId);
                return;
            }

            LoggerUtils.info("=== RECEIVED: {} for {} ===",
                    gameMessage.getMessageType(), gameMessage.getGameType());

            switch (gameMessage.getMessageType()) {
                case GAME_START -> handleGameStart(gameMessage);
                case GAME_STOP -> handleGameStop(gameMessage);
                case GAME_TIMEOUT -> handleGameTimeout(gameMessage);
                case ANSWER_CORRECT -> handleCorrectAnswer(gameMessage); // NEW!
                case PLAYER_ANSWER -> handlePlayerAnswer(gameMessage);
                default -> LoggerUtils.warn("Unknown message type: {}", gameMessage.getMessageType());
            }

        } catch (Exception exception) {
            LoggerUtils.error("Error handling Redis message: {}", exception.getMessage());
            exception.printStackTrace();
        }
    }

    private void handleGameStart(@NotNull GameMessage message) {
        String gameData = message.getPayload("gameData", String.class);
        Number startTimeNum = message.getPayload("startTime", Number.class);
        long startTime = startTimeNum != null ? startTimeNum.longValue() : System.currentTimeMillis();

        LoggerUtils.info("Starting remote game: {} with data: {}", message.getGameType(), gameData);
        proxyManager.handleRemoteGameStart(message.getGameType(), gameData, startTime);
    }

    private void handleGameStop(@NotNull GameMessage message) {
        LoggerUtils.info("Stopping remote game: {}", message.getGameType());
        proxyManager.handleRemoteGameStop(message.getGameType());
    }

    private void handleGameTimeout(@NotNull GameMessage message) {
        String correctAnswer = message.getPayload("correctAnswer", String.class);
        LoggerUtils.info("Remote game timeout: {} - Answer: {}", message.getGameType(), correctAnswer);
        proxyManager.handleRemoteGameTimeout(message.getGameType(), correctAnswer);
    }

    private void handleCorrectAnswer(@NotNull GameMessage message) {
        String playerName = message.getPayload("playerName", String.class);
        String answer = message.getPayload("answer", String.class);

        LoggerUtils.info("Correct answer broadcast: {} answered '{}' correctly for {}",
                playerName, answer, message.getGameType());

        proxyManager.handleCorrectAnswer(playerName, message.getGameType(), answer);
    }

    private void handlePlayerAnswer(@NotNull GameMessage message) {
        if (!proxyManager.isMasterServer()) {
            LoggerUtils.info("Ignoring player answer - not master server");
            return;
        }

        String playerName = message.getPayload("playerName", String.class);
        String answer = message.getPayload("answer", String.class);

        LoggerUtils.info("Master validating answer from {}: '{}'", playerName, answer);

        proxyManager.handlePlayerAnswerValidation(playerName, answer, message.getGameType());
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        LoggerUtils.info("✓ Subscribed to: {} (Total: {})", channel, subscribedChannels);
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        LoggerUtils.info("✗ Unsubscribed from: {} (Remaining: {})", channel, subscribedChannels);
    }
}