package com.playerpicked.chatgame.command;

import com.playerpicked.chatgame.game.GameService;
import com.playerpicked.chatgame.game.GameType;
import com.playerpicked.chatgame.game.crafting.CraftingTables;
import com.playerpicked.chatgame.text.MessageKey;
import com.playerpicked.chatgame.text.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.orphan.OrphanCommand;

public final class MainCommand implements OrphanCommand {
    private final GameService games;
    private final CraftingTables craftingTables;
    private final Messages messages;
    private final Runnable reloadPlugin;

    public MainCommand(@NotNull GameService games, @NotNull CraftingTables craftingTables, @NotNull Messages messages,
                       @NotNull Runnable reloadPlugin) {
        this.games = games;
        this.craftingTables = craftingTables;
        this.messages = messages;
        this.reloadPlugin = reloadPlugin;
    }

    @Subcommand("reload")
    @CommandPermission("chatgame.reload")
    public void reload(@NotNull CommandSender sender) {
        reloadPlugin.run();
        messages.send(sender, MessageKey.RELOADED);
    }

    @Subcommand("start")
    @CommandPermission("chatgame.start")
    public void start(@NotNull CommandSender sender, @NotNull GameType game) {
        MessageKey feedback = switch (games.start(game)) {
            case STARTED -> MessageKey.ROUND_STARTED;
            case NOT_CONFIGURED -> MessageKey.GAME_NOT_CONFIGURED;
            case NOT_MASTER_SERVER -> MessageKey.NOT_MASTER_SERVER;
        };
        messages.send(sender, feedback, Placeholder.unparsed("game", game.configSection()));
    }

    @Subcommand("crafting")
    public void openCraftingTable(@NotNull Player player) {
        if (!craftingTables.open(player)) messages.send(player, MessageKey.NO_CRAFTING_ROUND);
    }
}
