package com.mongenscave.mcchatgame.managers;

import com.mongenscave.mcchatgame.McChatGame;
import com.mongenscave.mcchatgame.identifiers.GameState;
import com.mongenscave.mcchatgame.identifiers.GameType;
import com.mongenscave.mcchatgame.identifiers.keys.ConfigKeys;
import com.mongenscave.mcchatgame.identifiers.keys.MessageKeys;
import com.mongenscave.mcchatgame.models.GameHandler;
import com.mongenscave.mcchatgame.models.impl.*;
import com.mongenscave.mcchatgame.services.MainThreadExecutorService;
import com.mongenscave.mcchatgame.utils.GameUtils;
import com.mongenscave.mcchatgame.utils.PlayerUtils;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * SIMPLE GameManager with answer validation and local processing
 */
public class GameManager {
    private static final Map<GameType, GameHandler> ACTIVE_GAMES = new EnumMap<>(GameType.class);
    @Getter private static long lastGameEndTime = 0;

    public static void startGame(@NotNull GameType type) {
        if (ACTIVE_GAMES.containsKey(type)) return;

        GameHandler handler = switch (type) {
            case MATH -> new GameMath();
            case WHO_AM_I -> new GameWhoAmI();
            case WORD_GUESSER -> new GameWordGuess();
            case RANDOM_CHARACTERS -> new GameRandomCharacters();
            case WORD_STOP -> new GameWordStop();
            case REVERSE -> new GameReverse();
            case FILL_OUT -> new GameFillOut();
            case CRAFTING -> new GameCrafting();
            case HANGMAN -> new GameHangman();
            case RANGE -> new GameRange();
        };

        handler.start();
        ACTIVE_GAMES.put(type, handler);
    }

    /**
     * Handle answer from player - used by master to validate
     */
    public static void handleAnswer(@NotNull Player player, @NotNull String answer) {
        ACTIVE_GAMES.values().forEach(handler -> {
            if (handler.getState() == GameState.ACTIVE) {
                handler.handleAnswer(player, answer);
            }
        });
    }

    /**
     * SIMPLE: Check if answer is correct (called by master for validation)
     */
    public static boolean isAnswerCorrect(@NotNull String answer, @NotNull GameType gameType) {
        GameHandler handler = ACTIVE_GAMES.get(gameType);

        if (handler == null || handler.getState() != GameState.ACTIVE) {
            return false;
        }

        // Get the correct answer from the game
        String correctAnswer = handler.getCorrectAnswer();

        if (correctAnswer == null) {
            return false;
        }

        // Simple comparison (can be overridden per game type if needed)
        return answer.trim().equalsIgnoreCase(correctAnswer.trim());
    }

    /**
     * SIMPLE: Handle local player win (when correct answer broadcast is received)
     * This processes rewards, database updates, etc. for LOCAL players only
     */
    public static void handleLocalPlayerWin(@NotNull Player player, @NotNull GameType gameType) {
        GameHandler handler = ACTIVE_GAMES.get(gameType);

        if (handler == null) {
            return;
        }

        long endTime = System.currentTimeMillis();
        double timeTaken = (endTime - handler.getStartTime()) / 1000.0;
        String formattedTime = String.format("%.2f", timeTaken);

        // Update database
        McChatGame.getInstance().getDatabase().incrementWin(player)
                .thenCompose(v -> McChatGame.getInstance().getDatabase().setTime(player, timeTaken))
                .thenAcceptAsync(v -> {
                    // Give rewards
                    GameUtils.rewardPlayer(player);

                    // Broadcast win message
                    GameUtils.broadcast(getWinMessage(gameType)
                            .replace("{player}", player.getName())
                            .replace("{time}", formattedTime));

                    // Handle streak
                    StreakManager.getInstance().onPlayerWin(player);

                    // Stop game
                    stopGame(gameType);
                }, MainThreadExecutorService.getInstance().getMainThreadExecutor());

        // Show toast and sound
        PlayerUtils.sendToast(player, ConfigKeys.TOAST_MESSAGE, ConfigKeys.TOAST_MATERIAL, ConfigKeys.TOAST_ENABLED);
        GameUtils.playSoundToWinner(player, ConfigKeys.SOUND_WIN_ENABLED, ConfigKeys.SOUND_WIN_SOUND);
    }

    public static int getActiveGameCount() {
        return (int) ACTIVE_GAMES.values().stream()
                .filter(handler -> handler.getState() == GameState.ACTIVE)
                .count();
    }

    public static void removeInactiveGames() {
        for (Map.Entry<GameType, GameHandler> entry : ACTIVE_GAMES.entrySet()) {
            if (entry.getValue().getState() == GameState.INACTIVE) {
                lastGameEndTime = System.currentTimeMillis();
                break;
            }
        }

        ACTIVE_GAMES.entrySet().removeIf(entry -> entry.getValue().getState() == GameState.INACTIVE);
    }

    public static void addGame(@NotNull GameType type, @NotNull GameHandler handler) {
        ACTIVE_GAMES.put(type, handler);
    }

    public static void stopAllGames() {
        ACTIVE_GAMES.values().forEach(GameHandler::stop);
        ACTIVE_GAMES.clear();
        lastGameEndTime = System.currentTimeMillis();
    }

    public static void stopGame(@NotNull GameType type) {
        GameHandler handler = ACTIVE_GAMES.get(type);
        if (handler != null) {
            handler.stop();
            ACTIVE_GAMES.remove(type);
            lastGameEndTime = System.currentTimeMillis();
        }
    }

    @NotNull
    private static String getWinMessage(@NotNull GameType gameType) {
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
}