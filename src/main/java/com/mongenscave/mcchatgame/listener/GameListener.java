package com.mongenscave.mcchatgame.listener;

import com.mongenscave.mcchatgame.McChatGame;
import com.mongenscave.mcchatgame.database.Database;
import com.mongenscave.mcchatgame.managers.GameManager;
import com.mongenscave.mcchatgame.managers.ProxyManager;
import com.mongenscave.mcchatgame.models.GameHandler;
import com.mongenscave.mcchatgame.models.impl.GameHangman;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

/**
 * SIMPLE GameListener:
 * - Master: process locally
 * - Slave: send to master for validation, master broadcasts result, slave processes locally
 */
@SuppressWarnings("deprecation")
public class GameListener implements Listener {
    @EventHandler
    public void onChat(final @NotNull AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().trim();
        Database database = McChatGame.getInstance().getDatabase();

        // Ensure player exists in database
        database.exists(player).thenAccept(exists -> {
            if (!exists) database.createPlayer(player);
        });

        if (message.startsWith("!")) {
            message = message.substring(1);
        }

        GameHandler currentGame = GameHandler.getCurrentActiveGame();

        if (currentGame == null) {
            return;
        }

        // Hangman validation
        if (currentGame instanceof GameHangman) {
            if (message.length() != 1 || !Character.isLetter(message.charAt(0))) {
                return;
            }
        }

        ProxyManager proxyManager = McChatGame.getInstance().getProxyManager();
        String finalMessage = message;

        if (proxyManager.isEnabled()) {
            if (proxyManager.isMasterServer()) {
                // MASTER: Process answer locally
                McChatGame.getInstance().getScheduler().runTask(() ->
                        GameManager.handleAnswer(player, finalMessage));
            } else {
                // SLAVE: Send answer to master, wait for validation broadcast
                proxyManager.sendAnswerToMaster(player, finalMessage, currentGame.getGameType());
            }
        } else {
            // NO REDIS: Process locally
            McChatGame.getInstance().getScheduler().runTask(() ->
                    GameManager.handleAnswer(player, finalMessage));
        }
    }
}