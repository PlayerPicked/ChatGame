package com.mongenscave.mcchatgame.managers;

import com.mongenscave.mcchatgame.McChatGame;
import com.mongenscave.mcchatgame.identifiers.GameType;
import com.mongenscave.mcchatgame.identifiers.keys.MessageKeys;
import com.mongenscave.mcchatgame.models.GameHandler;
import com.mongenscave.mcchatgame.models.impl.*;
import com.mongenscave.mcchatgame.proxy.RedisConfig;
import com.mongenscave.mcchatgame.proxy.RedisPublisher;
import com.mongenscave.mcchatgame.proxy.RedisSubscriber;
import com.mongenscave.mcchatgame.utils.GameUtils;
import com.mongenscave.mcchatgame.utils.LoggerUtils;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * SIMPLE ProxyManager - master validates, broadcasts result, all servers process locally
 */
public class ProxyManager {
    private final McChatGame plugin;
    @Getter private final RedisConfig redisConfig;
    @Getter private final RedisPublisher publisher;
    @Getter private final RedisSubscriber subscriber;
    @Getter private final String serverId;
    @Getter private boolean enabled;
    @Getter private boolean isMasterServer;

    private static final Map<GameType, GameHandlerFactory> GAME_FACTORIES = new EnumMap<>(GameType.class);

    static {
        GAME_FACTORIES.put(GameType.MATH, GameMath::new);
        GAME_FACTORIES.put(GameType.WHO_AM_I, GameWhoAmI::new);
        GAME_FACTORIES.put(GameType.WORD_GUESSER, GameWordGuess::new);
        GAME_FACTORIES.put(GameType.RANDOM_CHARACTERS, GameRandomCharacters::new);
        GAME_FACTORIES.put(GameType.WORD_STOP, GameWordStop::new);
        GAME_FACTORIES.put(GameType.REVERSE, GameReverse::new);
        GAME_FACTORIES.put(GameType.FILL_OUT, GameFillOut::new);
        GAME_FACTORIES.put(GameType.CRAFTING, GameCrafting::new);
        GAME_FACTORIES.put(GameType.HANGMAN, GameHangman::new);
        GAME_FACTORIES.put(GameType.RANGE, GameRange::new);
    }

    @FunctionalInterface
    private interface GameHandlerFactory {
        GameHandler create();
    }

    public ProxyManager(@NotNull McChatGame plugin) {
        this.plugin = plugin;
        this.serverId = plugin.getConfiguration().getString("redis.server-id",
                plugin.getServer().getPort() + "-" + System.currentTimeMillis());
        this.redisConfig = new RedisConfig(plugin);
        this.publisher = new RedisPublisher(redisConfig, serverId);
        this.subscriber = new RedisSubscriber(redisConfig, this, serverId);
        this.enabled = false;
        this.isMasterServer = false;
    }

    public void initialize() {
        if (!plugin.getConfiguration().getBoolean("redis.enabled", false)) {
            LoggerUtils.info("Redis cross-server support is disabled");
            enabled = false;
            isMasterServer = true;
            return;
        }

        String serverRole = plugin.getConfiguration().getString("redis.server-role", "master").toLowerCase();
        isMasterServer = serverRole.equals("master");

        if (!redisConfig.connect()) {
            LoggerUtils.error("Failed to connect to Redis - cross-server features disabled");
            enabled = false;
            isMasterServer = true;
            return;
        }

        if (!redisConfig.isConnected()) {
            LoggerUtils.error("Redis connection test failed");
            enabled = false;
            isMasterServer = true;
            return;
        }

        LoggerUtils.info("Redis connection successful, starting subscriber...");

        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}

        subscriber.subscribe();
        enabled = true;

        LoggerUtils.info("Redis cross-server support enabled");
        LoggerUtils.info("Server ID: {}", serverId);
        LoggerUtils.info("Server Role: {}", isMasterServer ? "MASTER (validates answers)" : "SLAVE (sends answers)");
    }

    public void shutdown() {
        if (enabled) {
            subscriber.shutdownSubscriber();
            redisConfig.disconnect();
            enabled = false;
            LoggerUtils.info("Redis cross-server support disabled");
        }
    }

    // ==================== OUTGOING MESSAGES ====================

    public void broadcastGameStart(@NotNull GameType gameType, @NotNull String gameData, long startTime) {
        if (!enabled || !isMasterServer) return;
        publisher.publishGameStart(gameType, gameData, startTime);
    }

    public void broadcastGameStop(@NotNull GameType gameType) {
        if (!enabled || !isMasterServer) return;
        publisher.publishGameStop(gameType);
    }

    public void broadcastGameTimeout(@NotNull GameType gameType, @NotNull String correctAnswer) {
        if (!enabled || !isMasterServer) return;
        publisher.publishGameTimeout(gameType, correctAnswer);
    }

    /**
     * SIMPLE: Broadcast that answer was correct - all servers will process locally
     */
    public void broadcastCorrectAnswer(@NotNull String playerName, @NotNull GameType gameType, @NotNull String answer) {
        if (!enabled || !isMasterServer) return;
        publisher.publishCorrectAnswer(playerName, gameType, answer);
    }

    /**
     * Slave sends answer to master for validation
     */
    public void sendAnswerToMaster(@NotNull Player player, @NotNull String answer, @NotNull GameType gameType) {
        if (!enabled || isMasterServer) return; // Only slaves send
        publisher.publishPlayerAnswer(player.getName(), answer, gameType);
    }

    // ==================== INCOMING MESSAGE HANDLERS ====================

    public void handleRemoteGameStart(@NotNull GameType gameType, @NotNull String gameData, long startTime) {
        plugin.getScheduler().runTask(() -> {
            LoggerUtils.info("Starting remote game: {}", gameType);
            GameManager.stopAllGames();

            GameHandler handler = createGameHandler(gameType);
            if (handler == null) return;

            handler.startAsRemote(startTime, gameData);
            GameManager.addGame(gameType, handler);
        });
    }

    public void handleRemoteGameStop(@NotNull GameType gameType) {
        plugin.getScheduler().runTask(() -> GameManager.stopGame(gameType));
    }

    public void handleRemoteGameTimeout(@NotNull GameType gameType, @NotNull String correctAnswer) {
        plugin.getScheduler().runTask(() -> {
            String message = getTimeoutMessage(gameType, correctAnswer);
            GameUtils.broadcast(message);
            GameManager.stopGame(gameType);
        });
    }

    /**
     * SIMPLE: Master validated answer - ALL servers process for their local players
     */
    public void handleCorrectAnswer(@NotNull String playerName, @NotNull GameType gameType, @NotNull String answer) {
        plugin.getScheduler().runTask(() -> {
            // Find local player
            Player localPlayer = Bukkit.getPlayerExact(playerName);

            if (localPlayer != null) {
                // Player is on THIS server - process the win!
                LoggerUtils.info("Processing correct answer for LOCAL player: {}", playerName);
                GameManager.handleLocalPlayerWin(localPlayer, gameType);
            } else {
                // Player is on another server - just display the win message
                LoggerUtils.info("Displaying win for REMOTE player: {}", playerName);
                String message = getWinMessage(gameType)
                        .replace("{player}", playerName)
                        .replace("{time}", "?"); // We don't have time here
                GameUtils.broadcast(message);
                GameManager.stopGame(gameType);
            }
        });
    }

    /**
     * MASTER ONLY: Validate answer from slave and broadcast if correct
     */
    public void handlePlayerAnswerValidation(@NotNull String playerName, @NotNull String answer, @NotNull GameType gameType) {
        plugin.getScheduler().runTask(() -> {
            LoggerUtils.info("Master validating answer from {}: '{}'", playerName, answer);

            // Check if answer is correct
            if (GameManager.isAnswerCorrect(answer, gameType)) {
                LoggerUtils.info("✓ Answer CORRECT! Broadcasting to all servers...");
                broadcastCorrectAnswer(playerName, gameType, answer);
            } else {
                LoggerUtils.info("✗ Answer INCORRECT: '{}'", answer);
                // Optionally: broadcast incorrect answer (for games like Hangman)
            }
        });
    }

    // ==================== HELPER METHODS ====================

    @NotNull
    private GameHandler createGameHandler(@NotNull GameType gameType) {
        GameHandlerFactory factory = GAME_FACTORIES.get(gameType);
        if (factory == null) {
            throw new IllegalArgumentException("No factory registered for game type: " + gameType);
        }
        return factory.create();
    }

    @NotNull
    private String getWinMessage(@NotNull GameType gameType) {
        return switch (gameType) {
            case MATH -> MessageKeys.MATH_GAME_WIN.getMessage();
            case WHO_AM_I -> MessageKeys.WHO_AM_I_WIN.getMessage();
            case WORD_GUESSER -> MessageKeys.WORD_GUESSER_WIN.getMessage();
            case RANDOM_CHARACTERS -> MessageKeys.RANDOM_CHARACTERS_WIN.getMessage();
            case WORD_STOP -> MessageKeys.WORD_STOP_WIN.getMessage();
            case REVERSE -> MessageKeys.REVERSE_WIN.getMessage();
            case FILL_OUT -> MessageKeys.FILL_OUT_WIN.getMessage();
            case CRAFTING -> MessageKeys.CRAFTING_WIN.getMessage();
            case HANGMAN -> MessageKeys.HANGMAN_WIN.getMessage();
            case RANGE -> MessageKeys.RANGE_WIN.getMessage();
        };
    }

    @NotNull
    private String getTimeoutMessage(@NotNull GameType gameType, @NotNull String correctAnswer) {
        String message = switch (gameType) {
            case MATH -> MessageKeys.MATH_GAME_NO_WIN.getMessage();
            case WHO_AM_I -> MessageKeys.WHO_AM_I_NO_WIN.getMessage();
            case WORD_GUESSER -> MessageKeys.WORD_GUESSER_NO_WIN.getMessage();
            case RANDOM_CHARACTERS -> MessageKeys.RANDOM_CHARACTERS_NO_WIN.getMessage();
            case WORD_STOP -> MessageKeys.WORD_STOP_NO_WIN.getMessage();
            case REVERSE -> MessageKeys.REVERSE_NO_WIN.getMessage();
            case FILL_OUT -> MessageKeys.FILL_OUT_NO_WIN.getMessage();
            case CRAFTING -> MessageKeys.CRAFTING_NO_WIN.getMessage();
            case HANGMAN -> MessageKeys.HANGMAN_NO_WIN.getMessage();
            case RANGE -> MessageKeys.RANGE_NO_WIN.getMessage();
        };

        return message.replace("{answer}", correctAnswer).replace("{word}", correctAnswer);
    }
}