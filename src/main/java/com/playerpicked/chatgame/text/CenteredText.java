package com.playerpicked.chatgame.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

public final class CenteredText {
    public static final String TAG = "[center]";

    private static final int CHAT_CENTER_PX = 154;
    private static final int SPACE_PX = 4;

    private CenteredText() {}

    public static boolean isCentered(@NotNull String line) {
        return line.startsWith(TAG);
    }

    public static @NotNull String strip(@NotNull String line) {
        return line.substring(TAG.length());
    }

    public static @NotNull Component center(@NotNull Component component) {
        int width = width(component, false);
        int padding = (CHAT_CENTER_PX - width / 2) / SPACE_PX;
        if (padding <= 0) return component;
        return Component.text(" ".repeat(padding)).append(component);
    }

    private static int width(@NotNull Component component, boolean parentBold) {
        TextDecoration.State state = component.decoration(TextDecoration.BOLD);
        boolean bold = state == TextDecoration.State.NOT_SET ? parentBold : state == TextDecoration.State.TRUE;

        int width = 0;
        if (component instanceof TextComponent text) {
            for (char c : text.content().toCharArray()) {
                if (c == '\n') continue;
                width += charWidth(c) + (bold ? 1 : 0);
            }
        }
        for (Component child : component.children()) {
            width += width(child, bold);
        }
        return width;
    }

    private static int charWidth(char c) {
        return switch (c) {
            case 'i', '!', ',', '.', ':', ';', '|', '\'' -> 2;
            case 'l', '`' -> 3;
            case 'I', 't', '[', ']', ' ' -> 4;
            case 'f', 'k', '<', '>', '(', ')', '{', '}', '"', '*' -> 5;
            case '@', '~' -> 7;
            default -> 6;
        };
    }
}
