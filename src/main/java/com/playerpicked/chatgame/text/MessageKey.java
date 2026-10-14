package com.playerpicked.chatgame.text;

import org.jetbrains.annotations.NotNull;

public enum MessageKey {
    RELOADED("general.reloaded"),
    NO_PERMISSION("general.no-permission"),
    ACTION_FAILED("general.action-failed"),
    UPDATE_AVAILABLE("general.update-available"),

    ROUND_STARTED("commands.round-started"),
    GAME_NOT_CONFIGURED("commands.game-not-configured"),
    NOT_MASTER_SERVER("commands.not-master-server"),
    NO_CRAFTING_ROUND("commands.no-crafting-round"),

    CRAFTING_MENU_TITLE("games.crafting.menu-title"),
    TOAST_TITLE("toast.title"),

    STREAK_REACHED("streaks.reached"),
    STREAK_LOST("streaks.lost"),

    PLACEHOLDER_NO_GAME("placeholders.no-game");

    private final String route;

    MessageKey(@NotNull String route) {
        this.route = route;
    }

    public @NotNull String route() {
        return route;
    }
}
