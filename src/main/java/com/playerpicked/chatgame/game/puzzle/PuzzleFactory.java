package com.playerpicked.chatgame.game.puzzle;

import com.playerpicked.chatgame.config.GameSettings;
import com.playerpicked.chatgame.config.PluginConfig;
import com.playerpicked.chatgame.game.GameType;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

public final class PuzzleFactory {
    private static final int DEFAULT_RANDOM_CHARACTERS_LENGTH = 8;
    private static final int DEFAULT_HANGMAN_WRONG_GUESSES = 6;

    private final PluginConfig config;
    private final Logger logger;

    public PuzzleFactory(@NotNull PluginConfig config, @NotNull Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public @NotNull Optional<Puzzle> create(@NotNull GameType type) {
        GameSettings settings = config.game(type.configSection());
        RandomGenerator random = ThreadLocalRandom.current();

        return switch (type) {
            case MATH -> pairPuzzle(type, pick(settings.entries("problems"), random));
            case WHO_AM_I -> pairPuzzle(type, pick(settings.entries("words"), random));
            case WORD_STOP -> pairPuzzle(type, pick(settings.entries("mobs"), random));
            case WORD_GUESSER -> pick(settings.entries("words"), random)
                    .map(word -> new TextPuzzle(type, WordTransforms.scramble(word, random), word));
            case REVERSE -> pick(settings.entries("words"), random)
                    .map(word -> new TextPuzzle(type, WordTransforms.reverse(word), word));
            case FILL_OUT -> pick(settings.entries("words"), random)
                    .map(word -> new TextPuzzle(type, WordTransforms.mask(word, random), word));
            case RANDOM_CHARACTERS -> {
                int length = Math.clamp(settings.getInt("length", DEFAULT_RANDOM_CHARACTERS_LENGTH), 1, 64);
                String sequence = WordTransforms.randomSequence(length, random);
                yield Optional.of(new TextPuzzle(type, sequence, sequence));
            }
            case RANGE -> rangePuzzle(settings, random);
            case HANGMAN -> pick(settings.entries("words"), random).map(word -> new HangmanPuzzle(word,
                    settings.getInt("max-wrong-guesses", DEFAULT_HANGMAN_WRONG_GUESSES), settings.getBoolean("only-once", false)));
            case CRAFTING -> craftingPuzzle(settings, random);
        };
    }

    public @NotNull Optional<Puzzle> restore(@NotNull GameType type, @NotNull List<String> seed) {
        return switch (type) {
            case MATH, WHO_AM_I, WORD_STOP, WORD_GUESSER, REVERSE, FILL_OUT, RANDOM_CHARACTERS -> TextPuzzle.fromSeed(type, seed);
            case RANGE -> RangePuzzle.fromSeed(seed);
            case HANGMAN -> HangmanPuzzle.fromSeed(seed);
            case CRAFTING -> CraftingPuzzle.fromSeed(seed);
        };
    }

    private @NotNull Optional<Puzzle> pairPuzzle(@NotNull GameType type, @NotNull Optional<String> entry) {
        return entry.flatMap(text -> {
            Optional<WordTransforms.TextPair> pair = WordTransforms.splitPair(text);
            if (pair.isEmpty()) logger.warn("Ignoring {} entry without '<question> = <answer>' format: {}", type.configSection(), text);
            return pair.map(parts -> new TextPuzzle(type, parts.left(), parts.right()));
        });
    }

    private @NotNull Optional<Puzzle> rangePuzzle(@NotNull GameSettings settings, @NotNull RandomGenerator random) {
        Optional<WordTransforms.TextPair> bounds = WordTransforms.splitPair(settings.getString("range", "0-20").replaceFirst("-", "="));
        try {
            int min = Integer.parseInt(bounds.orElseThrow().left());
            int max = Integer.parseInt(bounds.orElseThrow().right());
            if (min >= max) throw new IllegalArgumentException("min must be lower than max");
            return Optional.of(new RangePuzzle(random.nextInt(min, max + 1), min, max));
        } catch (RuntimeException exception) {
            logger.warn("range.range must look like '0-20': {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private @NotNull Optional<Puzzle> craftingPuzzle(@NotNull GameSettings settings, @NotNull RandomGenerator random) {
        Section crafts = settings.getSection("crafts");
        if (crafts == null) return Optional.empty();

        List<String> items = crafts.getRoutesAsStrings(false).stream().toList();
        return pick(items, random).flatMap(item -> {
            Optional<CraftingPuzzle> puzzle = CraftingPuzzle.parse(item, crafts.getStringList(item + ".items-to-place"));
            if (puzzle.isEmpty()) logger.warn("Ignoring crafting entry {}: unknown item or ingredient", item);
            return puzzle.map(Puzzle.class::cast);
        });
    }

    private static @NotNull Optional<String> pick(@NotNull List<String> entries, @NotNull RandomGenerator random) {
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.get(random.nextInt(entries.size())));
    }
}
