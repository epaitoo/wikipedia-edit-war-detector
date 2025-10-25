package com.epaitoo.springboot.entity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

@Data
@Slf4j
public class PageEditWindow {
    private static final int MAX_EDITS = 50; // Safety cap
    private static final long ONE_HOUR_SECONDS = 3600L; // 1 hour in seconds

    // State
    private String pageTitle;
    private Deque<WikimediaEditEvent> edits;

    public PageEditWindow(String pageTitle) {
        this.pageTitle = pageTitle;
        this.edits = new ArrayDeque<>();
    }

    /**
     * Add a new edit to the window
     */
    public void addEdit(WikimediaEditEvent edit) {
        // Remove expired edits first
        removeExpiredEdits(edit.getTimestamp());

        // Add new edit to the end
        edits.addLast(edit);

        // Safety cap to prevent memory explosion
        if (edits.size() > MAX_EDITS) {
            edits.removeFirst();
            log.warn("Page {} exceeded max edits, removed oldest", pageTitle);
        }
    }

    /**
     * Remove edits older than 1 hour
     */
    private void removeExpiredEdits(Long currentTimestamp) {
        while (!edits.isEmpty()) {
            WikimediaEditEvent oldest = edits.peekFirst();
            if (oldest != null && (currentTimestamp - oldest.getTimestamp()) >= ONE_HOUR_SECONDS) {
                edits.removeFirst();
                log.debug("Removed expired edit from page {}", pageTitle);
            } else {
                break;
            }
        }
    }

    /**
     * Check if this page is experiencing an edit war
     *
     * Criteria:
     * - At least 5 edits in the window
     * - By 2-3 distinct HUMAN editors (bots excluded)
     * - On main namespace articles (not talk pages, files, etc.)
     * - At least 50% of edits show conflict patterns
     */
    public boolean isEditWar() {
        // Need at least 5 edits
        if (edits.size() < 5) {
            return false;
        }

        // Count distinct human users in main namespace
        Set<String> users = new HashSet<>();
        for (WikimediaEditEvent edit : edits) {
            if (edit.isHumanEdit() && edit.isMainNamespace()) {
                users.add(edit.getUsername());
            }
        }

        // Need 2-3 distinct human users
        if (users.size() < 2 || users.size() > 3) {
            log.debug("Page {} has {} users (need 2-3 humans)", pageTitle, users.size());
            return false;
        }

        // Count conflict edits (reverts + opposing)
        int conflictEdits = countConflictEdits();
        double conflictRatio = (double) conflictEdits / edits.size();

        // Log when close to threshold (for debugging)
        if (edits.size() >= 5) {
            log.info("📊 Page {} - {} edits, {} users, {} conflicts ({}%)",
                    pageTitle, edits.size(), users.size(), conflictEdits,
                    (int)(conflictRatio * 100));
        }

        // At least 50% must be conflicts
        return conflictRatio >= 0.5;
    }

    /**
     * Count edits that show conflict behavior
     */
    private int countConflictEdits() {
        if (edits.size() < 2) {
            return 0;
        }

        int conflicts = 0;
        WikimediaEditEvent[] editArray = edits.toArray(new WikimediaEditEvent[0]);
        Set<Integer> previousLengths = new HashSet<>();

        for (int i = 0; i < editArray.length; i++) {
            WikimediaEditEvent currentEdit = editArray[i];

            //  Only count human edits in main namespace
            if (!currentEdit.isHumanEdit() || !currentEdit.isMainNamespace()) {
                continue;
            }

            boolean isConflict = false;

            // Check for pure revert (strong signal)
            if (isPureRevert(currentEdit, previousLengths)) {
                isConflict = true;
                log.debug("Pure revert detected on page {}: {} -> {}",
                        pageTitle, currentEdit.getLengthOld(), currentEdit.getLengthNew());
            }
            // Check for opposing edit (medium signal)
            else if (i > 0 && isOpposingEdit(editArray[i - 1], currentEdit)) {
                isConflict = true;
                log.debug("Opposing edit detected on page {}", pageTitle);
            }

            if (isConflict) {
                conflicts++;
            }

            // Track this edit's lengths for future comparisons
            previousLengths.add(currentEdit.getLengthOld());
            previousLengths.add(currentEdit.getLengthNew());
        }

        return conflicts;
    }

    /**
     * Check if this edit opposes the previous edit's direction
     */
    private boolean isOpposingEdit(WikimediaEditEvent previous, WikimediaEditEvent current) {
        Integer prevChange = previous.getLengthChange();
        Integer currChange = current.getLengthChange();

        // One added content, the other removed content
        return (prevChange > 0 && currChange < 0) || (prevChange < 0 && currChange > 0);
    }

    /**
     * Check if this edit reverts to a previous length
     */
    private boolean isPureRevert(WikimediaEditEvent currentEdit, Set<Integer> previousLengths) {
        // If the new length matches any previous length, it's likely a revert
        return previousLengths.contains(currentEdit.getLengthNew());
    }

    /**
     * Get number of edits in window
     */
    public int getEditCount() {
        return edits.size();
    }

    /**
     * Get the actual number of conflict edits
     */
    public int getConflictEditCount() {
        return countConflictEdits();
    }

}
