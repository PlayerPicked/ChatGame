package com.playerpicked.chatgame.game.puzzle;

import com.playerpicked.chatgame.game.Answer;
import com.playerpicked.chatgame.game.AnswerSource;
import com.playerpicked.chatgame.game.GameType;
import com.playerpicked.chatgame.game.GuessResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuzzleTest {
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    @Test
    void textPuzzleIgnoresCaseExceptForRandomCharacters() {
        TextPuzzle math = new TextPuzzle(GameType.MATH, "5 x 5", "25");
        assertEquals(GuessResult.SOLVED, math.guess(ALICE, Answer.chat(" 25 ")));
        assertEquals(GuessResult.WRONG, math.guess(ALICE, Answer.chat("24")));

        TextPuzzle word = new TextPuzzle(GameType.REVERSE, "elppA", "Apple");
        assertEquals(GuessResult.SOLVED, word.guess(ALICE, Answer.chat("apple")));

        TextPuzzle copy = new TextPuzzle(GameType.RANDOM_CHARACTERS, "aB3", "aB3");
        assertEquals(GuessResult.WRONG, copy.guess(ALICE, Answer.chat("ab3")));
        assertEquals(GuessResult.SOLVED, copy.guess(ALICE, Answer.chat("aB3")));
    }

    @Test
    void textPuzzleRoundTripsThroughItsSeed() {
        TextPuzzle original = new TextPuzzle(GameType.WHO_AM_I, "I explode", "Creeper");
        assertEquals(original, TextPuzzle.fromSeed(GameType.WHO_AM_I, original.seed()).orElseThrow());
        assertTrue(TextPuzzle.fromSeed(GameType.MATH, List.of("only one")).isEmpty());
    }

    @Test
    void craftingAnswersFromChatAreIgnoredByTextPuzzles() {
        TextPuzzle math = new TextPuzzle(GameType.MATH, "1 + 1", "2");
        assertEquals(GuessResult.IGNORED, math.guess(ALICE, new Answer("2", AnswerSource.CRAFTING_TABLE)));
    }

    @Test
    void rangePuzzleHidesItsTargetAndParsesNumbers() {
        RangePuzzle range = new RangePuzzle(7, 0, 20);
        assertEquals("0 ↔ 20", range.hint());
        assertEquals(GuessResult.WRONG, range.guess(ALICE, Answer.chat("6")));
        assertEquals(GuessResult.IGNORED, range.guess(ALICE, Answer.chat("seven")));
        assertEquals(GuessResult.SOLVED, range.guess(ALICE, Answer.chat("07")));
        assertEquals(range, RangePuzzle.fromSeed(range.seed()).orElseThrow());
    }

    @Test
    void hangmanRevealsLettersAndSolves() {
        HangmanPuzzle hangman = new HangmanPuzzle("alma", 6, false);
        assertEquals("_ _ _ _", hangman.hint());

        assertEquals(GuessResult.PROGRESSED, hangman.guess(ALICE, Answer.chat("a")));
        assertEquals(GuessResult.IGNORED, hangman.guess(BOB, Answer.chat("A")));
        assertEquals(GuessResult.IGNORED, hangman.guess(BOB, Answer.chat("lm")));
        assertEquals("A _ _ A", hangman.hint());

        assertEquals(GuessResult.PROGRESSED, hangman.guess(BOB, Answer.chat("l")));
        assertEquals(GuessResult.SOLVED, hangman.guess(ALICE, Answer.chat("m")));
    }

    @Test
    void hangmanRevealsSpacesSoMultiWordAnswersCanBeSolved() {
        HangmanPuzzle hangman = new HangmanPuzzle("ice cream", 6, false);
        assertEquals("_ _ _   _ _ _ _ _", hangman.hint());
        for (String letter : List.of("i", "c", "e", "r", "a")) hangman.guess(ALICE, Answer.chat(letter));
        assertEquals(GuessResult.SOLVED, hangman.guess(ALICE, Answer.chat("m")));
    }

    @Test
    void hangmanIsLostAfterTooManyWrongLetters() {
        HangmanPuzzle hangman = new HangmanPuzzle("ab", 2, false);
        assertEquals(GuessResult.PROGRESSED, hangman.guess(ALICE, Answer.chat("x")));
        assertEquals(GuessResult.LOST, hangman.guess(ALICE, Answer.chat("y")));
    }

    @Test
    void hangmanAllowsOneLetterPerPlayerWhenConfigured() {
        HangmanPuzzle hangman = new HangmanPuzzle("abc", 6, true);
        assertEquals(GuessResult.PROGRESSED, hangman.guess(ALICE, Answer.chat("a")));
        assertEquals(GuessResult.IGNORED, hangman.guess(ALICE, Answer.chat("b")));
        assertEquals(GuessResult.PROGRESSED, hangman.guess(BOB, Answer.chat("b")));
    }

    @Test
    void hangmanProgressMirrorsToAnotherServer() {
        HangmanPuzzle master = new HangmanPuzzle("alma", 6, false);
        master.guess(ALICE, Answer.chat("a"));
        master.guess(ALICE, Answer.chat("z"));

        HangmanPuzzle follower = (HangmanPuzzle) HangmanPuzzle.fromSeed(master.seed()).orElseThrow();
        follower.applyProgress(master.progress());

        assertEquals(master.hint(), follower.hint());
        assertEquals(master.progress(), follower.progress());
        assertFalse(follower.isWorthSubmitting(Answer.chat("a")));
        assertTrue(follower.isWorthSubmitting(Answer.chat("m")));
    }

    @Test
    void wordTransformsKeepTheAnswerSolvable() {
        Random random = new Random(42);
        assertEquals("elppA", WordTransforms.reverse("Apple"));

        String scrambled = WordTransforms.scramble("Computer", random);
        assertNotEquals("Computer", scrambled);
        assertEquals("Computer".chars().sorted().boxed().toList(), scrambled.chars().sorted().boxed().toList());

        String masked = WordTransforms.mask("Island", random);
        assertEquals(3, masked.chars().filter(character -> character == '_').count());

        assertEquals(8, WordTransforms.randomSequence(8, random).length());
        assertEquals(new WordTransforms.TextPair("5 x 5", "25"), WordTransforms.splitPair("5 x 5 = 25").orElseThrow());
        assertTrue(WordTransforms.splitPair("no separator").isEmpty());
    }
}
