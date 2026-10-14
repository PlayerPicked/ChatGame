package com.playerpicked.chatgame.storage;

import com.playerpicked.chatgame.config.DatabaseSettings;
import com.playerpicked.chatgame.config.DatabaseType;
import com.playerpicked.chatgame.stats.PlayerStats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStatsRepositoryTest {
    @TempDir
    Path dataFolder;

    private Database database;
    private PlayerStatsRepository repository;

    @BeforeEach
    void connect() {
        DatabaseSettings settings = new DatabaseSettings(DatabaseType.H2, "", 0, "", "", "", 2);
        database = Database.connect(settings, dataFolder);
        repository = new PlayerStatsRepository(database.sql());
    }

    @AfterEach
    void close() {
        database.close();
    }

    @Test
    void recordWinCountsWinsAndKeepsTheFastestTime() {
        repository.recordWin("Alice", 4.5);
        repository.recordWin("Alice", 2.25);
        repository.recordWin("Alice", 9.0);

        PlayerStats alice = repository.findByNames(List.of("Alice")).getFirst();
        assertEquals(3, alice.wins());
        assertEquals(2.25, alice.fastestSeconds());
    }

    @Test
    void streaksGrowAndBestStreakFollowsWithoutOvershooting() {
        assertEquals(1, repository.incrementStreak("Bob"));
        assertEquals(2, repository.incrementStreak("Bob"));
        assertEquals(3, repository.incrementStreak("Bob"));

        PlayerStats bob = repository.findByNames(List.of("Bob")).getFirst();
        assertEquals(3, bob.currentStreak());
        assertEquals(3, bob.bestStreak());
    }

    @Test
    void resetReturnsOnlyLostStreaksAndKeepsBestStreak() {
        repository.incrementStreak("Bob");
        repository.incrementStreak("Bob");
        repository.recordWin("Carol", 3);

        Map<String, Integer> lost = repository.resetStreaks(List.of("Bob", "Carol", "Nobody"));

        assertEquals(Map.of("Bob", 2), lost);
        PlayerStats bob = repository.findByNames(List.of("Bob")).getFirst();
        assertEquals(0, bob.currentStreak());
        assertEquals(2, bob.bestStreak());
    }

    @Test
    void leaderboardsAreOrderedAndSkipPlayersWithoutData() {
        repository.recordWin("Alice", 5);
        repository.recordWin("Alice", 6);
        repository.recordWin("Bob", 1.5);
        repository.incrementStreak("StreakOnly");

        List<PlayerStats> mostWins = repository.findMostWins(10);
        assertEquals(List.of("Alice", "Bob"), mostWins.stream().map(PlayerStats::name).toList());

        List<PlayerStats> fastest = repository.findFastest(10);
        assertEquals(List.of("Bob", "Alice"), fastest.stream().map(PlayerStats::name).toList());
        assertTrue(fastest.stream().allMatch(PlayerStats::hasFastestTime));
    }

    @Test
    void emptyNameListsDoNotQuery() {
        assertTrue(repository.resetStreaks(List.of()).isEmpty());
        assertTrue(repository.findByNames(List.of()).isEmpty());
        assertFalse(repository.findMostWins(1).iterator().hasNext());
    }

    @Test
    void reopeningKeepsExistingData() {
        repository.recordWin("Alice", 7);
        database.close();

        DatabaseSettings settings = new DatabaseSettings(DatabaseType.H2, "", 0, "", "", "", 2);
        database = Database.connect(settings, dataFolder);
        repository = new PlayerStatsRepository(database.sql());

        assertEquals(1, repository.findByNames(List.of("Alice")).getFirst().wins());
    }
}
