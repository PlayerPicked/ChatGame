package com.playerpicked.chatgame.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {

    @Test
    void comparesVersionsNumerically() {
        assertTrue(UpdateChecker.compareVersions("2.0.1", "2.0.0") > 0);
        assertTrue(UpdateChecker.compareVersions("1.10.0", "1.9.9") > 0);
        assertTrue(UpdateChecker.compareVersions("1.1.1", "2.0.0") < 0);
        assertEquals(0, UpdateChecker.compareVersions("2.0", "2.0.0"));
        assertEquals(0, UpdateChecker.compareVersions("2.0.0-SNAPSHOT", "2.0.0"));
    }
}
