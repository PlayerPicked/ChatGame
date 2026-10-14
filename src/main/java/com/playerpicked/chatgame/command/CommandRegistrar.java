package com.playerpicked.chatgame.command;

import com.playerpicked.chatgame.text.MessageKey;
import com.playerpicked.chatgame.text.Messages;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.exception.BukkitExceptionHandler;
import revxrsal.commands.exception.NoPermissionException;
import revxrsal.commands.orphan.Orphans;

import java.util.List;

public final class CommandRegistrar {
    private CommandRegistrar() { }

    public static void register(@NotNull JavaPlugin plugin, @NotNull List<String> aliases, @NotNull Messages messages,
                                @NotNull MainCommand mainCommand) {
        Lamp<BukkitCommandActor> lamp = BukkitLamp.builder(plugin)
                .exceptionHandler(new MessageExceptionHandler(messages))
                .build();

        lamp.register(Orphans.path(aliases.toArray(String[]::new)).handler(mainCommand));
    }

    private static final class MessageExceptionHandler extends BukkitExceptionHandler {
        private final Messages messages;

        private MessageExceptionHandler(@NotNull Messages messages) {
            this.messages = messages;
        }

        @Override
        public void onNoPermission(@NotNull NoPermissionException exception, @NotNull BukkitCommandActor actor) {
            messages.send(actor.sender(), MessageKey.NO_PERMISSION);
        }
    }
}
