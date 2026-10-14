package com.mongenscave.mcchatgame.models;

import com.mongenscave.mcchatgame.McChatGame;
import com.mongenscave.mcchatgame.identifiers.GameState;
import com.mongenscave.mcchatgame.identifiers.GameType;
import com.mongenscave.mcchatgame.managers.GameManager;
import com.mongenscave.mcchatgame.managers.ProxyManager;
import com.mongenscave.mcchatgame.managers.StreakManager;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * SIMPLE GameHandler - games provide correct answer for validation
 */
public abstract class GameHandler {
    @Getter protected GameState state = GameState.INACTIVE;
    protected Object gameData;
    protected boolean gameStarted = false;
    @Getter protected boolean isRemoteGame = false;
    @Getter protected long startTime;

    @Getter protected String remoteGameData = null;

    private static GameHandler currentActiveGame = null;

    public abstract void start();
    public abstract void stop();
    public abstract void handleAnswer(@NotNull Player player, @NotNull String answer);
    public abstract GameType getGameType();

    /**
     * SIMPLE: Return the correct answer for this game
     * Used by master server to validate answers from slaves
     */
    @Nullable
    public abstract String getCorrectAnswer();

    public void startAsRemote(long remoteStartTime, @NotNull String remoteGameData) {
        this.isRemoteGame = true;
        this.startTime = remoteStartTime;
        this.remoteGameData = remoteGameData;
        this.state = GameState.INACTIVE;
        start();
    }

    protected void cleanup() {
        state = GameState.INACTIVE;
        gameData = null;
        gameStarted = false;
        isRemoteGame = false;
        startTime = 0;
        remoteGameData = null;

        if (currentActiveGame == this) currentActiveGame = null;

        GameManager.removeInactiveGames();

        ProxyManager proxyManager = McChatGame.getInstance().getProxyManager();
        if (proxyManager.isEnabled() && proxyManager.isMasterServer()) {
            proxyManager.broadcastGameStop(getGameType());
        }
    }

    protected void setAsActive() {
        currentActiveGame = this;
        state = GameState.ACTIVE;

        if (!isRemoteGame) {
            startTime = System.currentTimeMillis();
        }

        if (!gameStarted) {
            StreakManager.getInstance().onGameStart();
            gameStarted = true;

            ProxyManager proxyManager = McChatGame.getInstance().getProxyManager();
            if (!isRemoteGame && proxyManager.isEnabled() && proxyManager.isMasterServer()) {
                String dataToSend = getOriginalGameData();
                proxyManager.broadcastGameStart(
                        getGameType(),
                        dataToSend != null ? dataToSend : "",
                        startTime
                );
            }
        }
    }

    protected String getOriginalGameData() {
        return getGameData();
    }

    protected void handlePlayerWin(@NotNull Player winner) {
        StreakManager.getInstance().onPlayerWin(winner);

        ProxyManager proxyManager = McChatGame.getInstance().getProxyManager();
        if (proxyManager.isEnabled() && proxyManager.isMasterServer()) {
            // SIMPLE: Just broadcast that this answer was correct
            // All servers will process locally
            proxyManager.broadcastCorrectAnswer(winner.getName(), getGameType(), getCorrectAnswer());
        }
    }

    protected void handleGameTimeout() {
        StreakManager.getInstance().onGameTimeout();
    }

    @Nullable
    public static GameHandler getCurrentActiveGame() {
        return currentActiveGame;
    }

    @Nullable
    public String getGameData() {
        return gameData != null ? gameData.toString() : null;
    }
}