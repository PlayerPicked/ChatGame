package com.playerpicked.chatgame.game.puzzle;

import com.playerpicked.chatgame.game.Answer;
import com.playerpicked.chatgame.game.GameType;
import com.playerpicked.chatgame.game.GuessResult;
import com.playerpicked.chatgame.text.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class HangmanPuzzle implements Puzzle {
    private final String word;
    private final int maxWrongGuesses;
    private final boolean oneGuessPerPlayer;
    private final Set<Character> guessedLetters = new LinkedHashSet<>();
    private final Set<UUID> guessers = new HashSet<>();
    private int wrongGuesses;

    public HangmanPuzzle(@NotNull String word, int maxWrongGuesses, boolean oneGuessPerPlayer) {
        this.word = word.trim().toUpperCase(Locale.ROOT);
        this.maxWrongGuesses = Math.max(1, maxWrongGuesses);
        this.oneGuessPerPlayer = oneGuessPerPlayer;
    }

    public static @NotNull Optional<Puzzle> fromSeed(@NotNull List<String> seed) {
        if (seed.size() != 3 || seed.getFirst().isBlank()) return Optional.empty();
        try {
            return Optional.of(new HangmanPuzzle(seed.get(0), Integer.parseInt(seed.get(1)), Boolean.parseBoolean(seed.get(2))));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public @NotNull GameType type() {
        return GameType.HANGMAN;
    }

    @Override
    public @NotNull @Unmodifiable List<String> seed() {
        return List.of(word, String.valueOf(maxWrongGuesses), String.valueOf(oneGuessPerPlayer));
    }

    @Override
    public synchronized @NotNull String hint() {
        return word.chars()
                .mapToObj(character -> isRevealed((char) character) ? String.valueOf((char) character) : "_")
                .collect(Collectors.joining(" "));
    }

    @Override
    public @NotNull String solution() {
        return word;
    }

    @Override
    public synchronized @NotNull GuessResult guess(@NotNull UUID playerUuid, @NotNull Answer answer) {
        Optional<Character> letter = letterOf(answer);
        if (letter.isEmpty() || guessedLetters.contains(letter.get())) return GuessResult.IGNORED;
        if (oneGuessPerPlayer && !guessers.add(playerUuid)) return GuessResult.IGNORED;

        guessedLetters.add(letter.get());
        if (word.indexOf(letter.get()) >= 0) return isComplete() ? GuessResult.SOLVED : GuessResult.PROGRESSED;

        wrongGuesses++;
        return wrongGuesses >= maxWrongGuesses ? GuessResult.LOST : GuessResult.PROGRESSED;
    }

    @Override
    public synchronized boolean isWorthSubmitting(@NotNull Answer answer) {
        return letterOf(answer).filter(letter -> !guessedLetters.contains(letter)).isPresent();
    }

    @Override
    public synchronized @NotNull @Unmodifiable List<String> progress() {
        String letters = guessedLetters.stream().map(String::valueOf).collect(Collectors.joining());
        return List.of(letters, String.valueOf(wrongGuesses));
    }

    @Override
    public synchronized void applyProgress(@NotNull List<String> progress) {
        if (progress.size() != 2) return;

        guessedLetters.clear();
        progress.get(0).chars().forEach(letter -> guessedLetters.add((char) letter));
        try {
            wrongGuesses = Integer.parseInt(progress.get(1));
        } catch (NumberFormatException ignored) {
            // keep the previous count; the letters are still worth showing
        }
    }

    @Override
    public synchronized @NotNull Component renderHint(@NotNull Messages messages) {
        List<Component> letters = new ArrayList<>();
        for (char character : word.toCharArray()) {
            if (!Character.isLetter(character)) letters.add(Component.text(character));
            else if (guessedLetters.contains(character)) letters.add(messages.single("games.hangman.found-letter",
                    Placeholder.unparsed("letter", String.valueOf(character))));
            else letters.add(messages.single("games.hangman.hidden-letter"));
        }
        return Component.join(JoinConfiguration.noSeparators(), letters);
    }

    @Override
    public synchronized @NotNull TagResolver extraPlaceholders(@NotNull Messages messages) {
        List<String> stageLines = messages.rawLines("games.hangman.stages");
        List<Component> stages = new ArrayList<>();
        for (int index = 0; index < stageLines.size(); index++) {
            String format = index < wrongGuesses ? "games.hangman.stage-reached" : "games.hangman.stage-pending";
            stages.add(messages.single(format, Placeholder.unparsed("line", stageLines.get(index))));
        }
        return Placeholder.component("stages", Component.join(JoinConfiguration.newlines(), stages));
    }

    private boolean isRevealed(char character) {
        return !Character.isLetter(character) || guessedLetters.contains(character);
    }

    private boolean isComplete() {
        return word.chars().allMatch(character -> isRevealed((char) character));
    }

    private static @NotNull Optional<Character> letterOf(@NotNull Answer answer) {
        String text = answer.text().trim();
        if (!answer.isFromChat() || text.length() != 1 || !Character.isLetter(text.charAt(0))) return Optional.empty();
        return Optional.of(text.toUpperCase(Locale.ROOT).charAt(0));
    }
}
