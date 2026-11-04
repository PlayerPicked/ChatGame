package com.playerpicked.chatgame.game.action;

import com.playerpicked.chatgame.game.Answer;
import com.playerpicked.chatgame.game.GameType;
import com.playerpicked.chatgame.game.GuessResult;
import com.playerpicked.chatgame.game.puzzle.Puzzle;
import com.playerpicked.chatgame.text.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class ActionPuzzle implements Puzzle {
    public static final String ANY_TARGET_MESSAGE = "actions.any-target";

    private final ActionKind kind;
    private final ActionKind.Target target;
    private final int amount;
    private final Map<UUID, Integer> counts = new ConcurrentHashMap<>();
    private final Set<String> placedBlocks = ConcurrentHashMap.newKeySet();

    public ActionPuzzle(@NotNull ActionKind kind, @NotNull ActionKind.Target target, int amount) {
        this.kind = kind;
        this.target = target;
        this.amount = Math.max(1, amount);
    }

    public static @NotNull Optional<Puzzle> fromSeed(@NotNull GameType type, @NotNull List<String> seed) {
        if (seed.size() != 2) return Optional.empty();

        Optional<ActionKind> kind = ActionKind.of(type);
        Optional<ActionKind.Target> target = kind.flatMap(action -> action.resolveTarget(seed.get(0)));
        if (target.isEmpty()) return Optional.empty();
        try {
            return Optional.of(new ActionPuzzle(kind.get(), target.get(), Integer.parseInt(seed.get(1))));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public @NotNull ActionKind kind() {
        return kind;
    }

    public int amount() {
        return amount;
    }

    public boolean matches(@NotNull ActionKind action, @NotNull String reportedTarget) {
        return kind == action && target.matches(reportedTarget);
    }

    public int record(@NotNull UUID playerUuid) {
        return counts.merge(playerUuid, 1, Integer::sum);
    }

    public boolean rememberPlacement(@NotNull String blockKey) {
        return placedBlocks.add(blockKey);
    }

    public boolean wasPlacedThisRound(@NotNull String blockKey) {
        return placedBlocks.contains(blockKey);
    }

    public @NotNull Answer completedAnswer() {
        return new Answer(target.name(), kind.source());
    }

    @Override
    public @NotNull GameType type() {
        return kind.game();
    }

    @Override
    public @NotNull @Unmodifiable List<String> seed() {
        return List.of(target.name(), String.valueOf(amount));
    }

    @Override
    public @NotNull String hint() {
        String name = Arrays.stream(target.name().toLowerCase(Locale.ROOT).split("_"))
                .map(word -> word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
        return amount + "x " + name;
    }

    @Override
    public @NotNull String solution() {
        return hint();
    }

    @Override
    public @NotNull GuessResult guess(@NotNull UUID playerUuid, @NotNull Answer answer) {
        return isWorthSubmitting(answer) ? GuessResult.SOLVED : GuessResult.IGNORED;
    }

    @Override
    public boolean isWorthSubmitting(@NotNull Answer answer) {
        return answer.source() == kind.source() && answer.text().equals(target.name());
    }

    @Override
    public @NotNull Component renderHint(@NotNull Messages messages) {
        return target.display() == null ? messages.single(ANY_TARGET_MESSAGE) : Component.translatable(target.display());
    }

    @Override
    public @NotNull TagResolver extraPlaceholders(@NotNull Messages messages) {
        return Placeholder.unparsed("amount", String.valueOf(amount));
    }
}
