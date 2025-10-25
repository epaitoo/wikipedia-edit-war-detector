package com.epaitoo.springboot.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PageEditWindowTest {
    private PageEditWindow window;
    private long baseTimestamp;

    @BeforeEach
    void setUp() {
        window = new PageEditWindow("Test_Article");
        baseTimestamp = System.currentTimeMillis() / 1000; // Current time in seconds
    }

    @Test
    @DisplayName("Should detect edit war with reverting pattern between 2 users")
    void testEditWar_WithRevertingPattern() {
        // User A adds 200 bytes
        window.addEdit(createEdit("Alice", 5000, 5200, baseTimestamp));

        // User B reverts (removes 200 bytes)
        window.addEdit(createEdit("Bob", 5200, 5000, baseTimestamp + 300));

        // User A reverts back (adds 200 bytes)
        window.addEdit(createEdit("Alice", 5000, 5200, baseTimestamp + 600));

        // User B reverts again
        window.addEdit(createEdit("Bob", 5200, 5000, baseTimestamp + 900));

        // User A reverts one more time (5th edit)
        window.addEdit(createEdit("Alice", 5000, 5200, baseTimestamp + 1200));

        // Should detect edit war (5 edits, 2 users, high conflict ratio)
        assertTrue(window.isEditWar(),
                "Should detect edit war with clear reverting pattern");
        assertEquals(5, window.getEditCount());
    }

    @Test
    @DisplayName("Should detect edit war with opposing edits between 3 users")
    void testEditWar_WithOpposingEdits() {
        // User A adds content
        window.addEdit(createEdit("Alice", 5000, 5300, baseTimestamp));

        // User B removes content
        window.addEdit(createEdit("Bob", 5300, 5100, baseTimestamp + 300));

        // User C adds content
        window.addEdit(createEdit("Charlie", 5100, 5400, baseTimestamp + 600));

        // User A removes content
        window.addEdit(createEdit("Alice", 5400, 5200, baseTimestamp + 900));

        // User B adds content (5th edit)
        window.addEdit(createEdit("Bob", 5200, 5500, baseTimestamp + 1200));

        // Should detect edit war (5 edits, 3 users, opposing changes)
        assertTrue(window.isEditWar(),
                "Should detect edit war with opposing edit pattern");
    }

    @Test
    @DisplayName("Should NOT detect war with collaborative edits")
    void testNoEditWar_CollaborativeEdits() {
        // Multiple users making non-conflicting edits
        window.addEdit(createEdit("Alice", 5000, 5100, baseTimestamp));
        window.addEdit(createEdit("Bob", 5100, 5200, baseTimestamp + 300));
        window.addEdit(createEdit("Charlie", 5200, 5300, baseTimestamp + 600));
        window.addEdit(createEdit("Alice", 5300, 5400, baseTimestamp + 900));
        window.addEdit(createEdit("Bob", 5400, 5500, baseTimestamp + 1200));

        // Should NOT detect edit war (all edits add content, no conflicts)
        assertFalse(window.isEditWar(),
                "Should not detect war when users are collaborating");
    }

    @Test
    @DisplayName("Should NOT detect war with only 1 user (vandalism revert)")
    void testNoEditWar_SingleUser() {
        // Same user making multiple edits
        window.addEdit(createEdit("Alice", 5000, 5200, baseTimestamp));
        window.addEdit(createEdit("Alice", 5200, 5400, baseTimestamp + 300));
        window.addEdit(createEdit("Alice", 5400, 5600, baseTimestamp + 600));
        window.addEdit(createEdit("Alice", 5600, 5800, baseTimestamp + 900));
        window.addEdit(createEdit("Alice", 5800, 6000, baseTimestamp + 1200));

        // Should NOT detect edit war (only 1 user)
        assertFalse(window.isEditWar(),
                "Should not detect war with only one user");
    }

    @Test
    @DisplayName("Should NOT detect war with too many users (4+)")
    void testNoEditWar_TooManyUsers() {
        // 4 different users (not edit war, just high activity)
        window.addEdit(createEdit("Alice", 5000, 5100, baseTimestamp));
        window.addEdit(createEdit("Bob", 5100, 5200, baseTimestamp + 300));
        window.addEdit(createEdit("Charlie", 5200, 5300, baseTimestamp + 600));
        window.addEdit(createEdit("David", 5300, 5400, baseTimestamp + 900));
        window.addEdit(createEdit("Eve", 5400, 5500, baseTimestamp + 1200));

        // Should NOT detect edit war (too many users = general activity)
        assertFalse(window.isEditWar(),
                "Should not detect war with 4+ users (general activity)");
    }

    @Test
    @DisplayName("Should NOT detect war with insufficient edits")
    void testNoEditWar_InsufficientEdits() {
        // Only 3 edits
        window.addEdit(createEdit("Alice", 5000, 5200, baseTimestamp));
        window.addEdit(createEdit("Bob", 5200, 5000, baseTimestamp + 300));
        window.addEdit(createEdit("Alice", 5000, 5200, baseTimestamp + 600));

        // Should NOT detect edit war (need at least 5 edits)
        assertFalse(window.isEditWar(),
                "Should not detect war with fewer than 5 edits");
    }

    @Test
    @DisplayName("Should filter out bot edits from detection")
    void testEditWar_BotEditsFiltered() {
        // 3 human edits + 2 bot edits
        window.addEdit(createEdit("Alice", 5000, 5200, baseTimestamp));
        window.addEdit(createBotEdit("CleanupBot", 5200, 5210, baseTimestamp + 300));
        window.addEdit(createEdit("Bob", 5210, 5000, baseTimestamp + 600));
        window.addEdit(createBotEdit("SpellBot", 5000, 5005, baseTimestamp + 900));
        window.addEdit(createEdit("Alice", 5005, 5200, baseTimestamp + 1200));

        // Should NOT detect war (only 3 human edits, need 5)
        assertFalse(window.isEditWar(),
                "Should filter out bot edits from conflict detection");
    }

    @Test
    @DisplayName("Should filter out non-main namespace edits")
    void testEditWar_NonMainNamespaceFiltered() {
        // 3 main namespace + 2 talk page edits
        window.addEdit(createEdit("Alice", 5000, 5200, baseTimestamp));
        window.addEdit(createEditWithNamespace("Bob", 5200, 5000, baseTimestamp + 300, 1)); // Talk page
        window.addEdit(createEdit("Alice", 5000, 5200, baseTimestamp + 600));
        window.addEdit(createEditWithNamespace("Bob", 5200, 5000, baseTimestamp + 900, 1)); // Talk page
        window.addEdit(createEdit("Alice", 5000, 5200, baseTimestamp + 1200));

        // Should NOT detect war (only 3 main namespace edits)
        assertFalse(window.isEditWar(),
                "Should filter out non-main namespace edits");
    }

    @Test
    @DisplayName("Should remove edits older than 1 hour")
    void testEditExpiration() {
        long oneHourAgo = baseTimestamp - 3600; // 1 hour = 3600 seconds
        long twoHoursAgo = baseTimestamp - 7200;

        // Add old edits
        window.addEdit(createEdit("Alice", 5000, 5100, twoHoursAgo));
        window.addEdit(createEdit("Bob", 5100, 5200, oneHourAgo));

        // Add current edit (should trigger cleanup)
        window.addEdit(createEdit("Charlie", 5200, 5300, baseTimestamp));

        // Should only have 1 edit (the current one, old ones removed)
        assertEquals(1, window.getEditCount(),
                "Should remove edits older than 1 hour");
    }

    @Test
    @DisplayName("Should get correct conflict count")
    void testConflictEditCount() {
        // Create reverting pattern
        window.addEdit(createEdit("Alice", 5000, 5200, baseTimestamp));
        window.addEdit(createEdit("Bob", 5200, 5000, baseTimestamp + 300)); // Conflict
        window.addEdit(createEdit("Alice", 5000, 5200, baseTimestamp + 600)); // Conflict
        window.addEdit(createEdit("Bob", 5200, 5000, baseTimestamp + 900)); // Conflict
        window.addEdit(createEdit("Alice", 5000, 5200, baseTimestamp + 1200)); // Conflict

        // Should have 4 conflicts (all but the first edit)
        int conflicts = window.getConflictEditCount();
        assertTrue(conflicts >= 3,
                "Should detect multiple conflicts in reverting pattern");
    }

    // Helper methods to create test data

    private WikimediaEditEvent createEdit(String username, int oldLength, int newLength, long timestamp) {
        return createEditWithNamespace(username, oldLength, newLength, timestamp, 0);
    }

    private WikimediaEditEvent createBotEdit(String username, int oldLength, int newLength, long timestamp) {
        WikimediaEditEvent event = createEdit(username, oldLength, newLength, timestamp);
        event.setIsBot(true);
        return event;
    }

    private WikimediaEditEvent createEditWithNamespace(String username, int oldLength,
                                                       int newLength, long timestamp, int namespace) {
        WikimediaEditEvent event = new WikimediaEditEvent();
        event.setPageTitle("Test_Article");
        event.setUsername(username);
        event.setLengthOld(oldLength);
        event.setLengthNew(newLength);
        event.setTimestamp(timestamp);
        event.setIsBot(false);
        event.setNamespace(namespace);
        event.setType("edit");
        event.setWiki("en.wikipedia.org");
        return event;
    }

}
