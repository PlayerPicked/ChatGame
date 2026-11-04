package com.playerpicked.chatgame.game.action;

import com.playerpicked.chatgame.game.AnswerSource;
import com.playerpicked.chatgame.game.GameType;
import net.kyori.adventure.translation.Translatable;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ActionKind {
    BREAK_BLOCK(GameType.BREAK_BLOCK, AnswerSource.BLOCK_BREAK, TargetKind.BLOCK),
    PLACE_BLOCK(GameType.PLACE_BLOCK, AnswerSource.BLOCK_PLACE, TargetKind.BLOCK),
    KILL_MOB(GameType.KILL_MOB, AnswerSource.MOB_KILL, TargetKind.MOB),
    KILL_PLAYER(GameType.KILL_PLAYER, AnswerSource.PLAYER_KILL, TargetKind.PLAYER),
    CATCH_FISH(GameType.CATCH_FISH, AnswerSource.FISH_CATCH, TargetKind.ITEM),
    CONSUME_ITEM(GameType.CONSUME_ITEM, AnswerSource.ITEM_CONSUME, TargetKind.ITEM),
    BREED_ANIMAL(GameType.BREED_ANIMAL, AnswerSource.ANIMAL_BREED, TargetKind.MOB);

    private static final String MINECRAFT_NAMESPACE = "MINECRAFT:";

    private final GameType game;
    private final AnswerSource source;
    private final TargetKind targetKind;

    ActionKind(@NotNull GameType game, @NotNull AnswerSource source, @NotNull TargetKind targetKind) {
        this.game = game;
        this.source = source;
        this.targetKind = targetKind;
    }

    public static @NotNull Optional<ActionKind> of(@NotNull GameType game) {
        return Arrays.stream(values()).filter(kind -> kind.game == game).findFirst();
    }

    public @NotNull GameType game() {
        return game;
    }

    public @NotNull AnswerSource source() {
        return source;
    }

    /**
     * Resolves a config target such as {@code DIAMOND_ORE}, {@code ZOMBIE} or {@code ANY}.
     * Empty when the name is unknown or does not fit this action (e.g. a non-block for {@link #BREAK_BLOCK}).
     */
    public @NotNull Optional<Target> resolveTarget(@NotNull String name) {
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith(MINECRAFT_NAMESPACE)) normalized = normalized.substring(MINECRAFT_NAMESPACE.length());
        if (normalized.equals(Target.ANY.name())) return Optional.of(Target.ANY);
        return targetKind.resolve(normalized);
    }

    /**
     * {@code name} is the canonical enum name the listeners report, {@code display} is translated by the client.
     * {@link #ANY} matches every block, item or mob of the action.
     */
    public record Target(@NotNull String name, @Nullable Translatable display) {
        public static final Target ANY = new Target("ANY", null);

        public boolean matches(@NotNull String reported) {
            return equals(ANY) || name.equals(reported);
        }
    }

    private enum TargetKind {
        BLOCK {
            @Override
            Optional<Target> resolve(@NotNull String name) {
                Material material = Material.matchMaterial(name);
                return material != null && material.isBlock() ? Optional.of(new Target(material.name(), material)) : Optional.empty();
            }
        },
        ITEM {
            @Override
            Optional<Target> resolve(@NotNull String name) {
                Material material = Material.matchMaterial(name);
                return material != null && material.isItem() ? Optional.of(new Target(material.name(), material)) : Optional.empty();
            }
        },
        MOB {
            @Override
            Optional<Target> resolve(@NotNull String name) {
                return entity(name).filter(type -> type.isAlive() && type != EntityType.PLAYER).map(type -> new Target(type.name(), type));
            }
        },
        PLAYER {
            @Override
            Optional<Target> resolve(@NotNull String name) {
                return entity(name).filter(type -> type == EntityType.PLAYER).map(type -> new Target(type.name(), type));
            }
        };

        abstract Optional<Target> resolve(@NotNull String name);

        private static Optional<EntityType> entity(@NotNull String name) {
            try {
                return Optional.of(EntityType.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }
    }
}
