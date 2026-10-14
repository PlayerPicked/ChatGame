package com.playerpicked.chatgame.game.puzzle;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class WordTransforms {
    private static final String RANDOM_CHARACTER_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^*()";
    private static final int SCRAMBLE_ATTEMPTS = 10;

    private WordTransforms() { }

    public static @NotNull Optional<TextPair> splitPair(@NotNull String entry) {
        int separator = entry.indexOf('=');
        if (separator < 0) return Optional.empty();

        String left = entry.substring(0, separator).trim();
        String right = entry.substring(separator + 1).trim();
        return left.isEmpty() || right.isEmpty() ? Optional.empty() : Optional.of(new TextPair(left, right));
    }

    public static @NotNull String reverse(@NotNull String word) {
        return new StringBuilder(word).reverse().toString();
    }

    public static @NotNull String scramble(@NotNull String sentence, @NotNull RandomGenerator random) {
        return Arrays.stream(sentence.split(" "))
                .map(word -> scrambleWord(word, random))
                .collect(Collectors.joining(" "));
    }

    public static @NotNull String mask(@NotNull String word, @NotNull RandomGenerator random) {
        List<Integer> maskable = IntStream.range(0, word.length())
                .filter(index -> !Character.isWhitespace(word.charAt(index)))
                .boxed()
                .collect(Collectors.toCollection(ArrayList::new));
        shuffle(maskable, random);

        int hiddenCount = Math.max(1, (int) Math.ceil(maskable.size() / 2.0));
        char[] characters = word.toCharArray();
        maskable.stream().limit(hiddenCount).forEach(index -> characters[index] = '_');
        return new String(characters);
    }

    public static @NotNull String randomSequence(int length, @NotNull RandomGenerator random) {
        StringBuilder sequence = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            sequence.append(RANDOM_CHARACTER_POOL.charAt(random.nextInt(RANDOM_CHARACTER_POOL.length())));
        }
        return sequence.toString();
    }

    private static @NotNull String scrambleWord(@NotNull String word, @NotNull RandomGenerator random) {
        if (word.chars().distinct().count() < 2) return word;

        List<Character> characters = word.chars().mapToObj(character -> (char) character)
                .collect(Collectors.toCollection(ArrayList::new));
        for (int attempt = 0; attempt < SCRAMBLE_ATTEMPTS; attempt++) {
            shuffle(characters, random);
            String scrambled = characters.stream().map(String::valueOf).collect(Collectors.joining());
            if (!scrambled.equals(word)) return scrambled;
        }
        return reverse(word);
    }

    private static <T> void shuffle(@NotNull List<T> values, @NotNull RandomGenerator random) {
        for (int index = values.size() - 1; index > 0; index--) {
            Collections.swap(values, index, random.nextInt(index + 1));
        }
    }

    public record TextPair(@NotNull String left, @NotNull String right) { }
}
