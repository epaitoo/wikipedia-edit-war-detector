package com.epaitoo.springboot.service;

import com.epaitoo.springboot.entity.EditWarAlert;
import com.epaitoo.springboot.entity.EditWarStatus;
import com.epaitoo.springboot.entity.PageEditWindow;
import com.epaitoo.springboot.entity.WikimediaEditEvent;
import com.epaitoo.springboot.persistence.entity.EditWarAlertEntity;
import com.epaitoo.springboot.persistence.mapper.AlertMapper;
import com.epaitoo.springboot.persistence.repository.EditWarAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
@RequiredArgsConstructor
public class EditWarDetectionService {

    // Dependencies
    private final EditWarAlertRepository alertRepository;
    private final AlertMapper alertMapper;

    // Store edit windows per page (thread-safe)
    private final Map<String, PageEditWindow> pageWindows = new ConcurrentHashMap<>();

    /**
     * Process a new edit event
     * Returns an EditWarAlert if a war is detected, null otherwise
     */
    @Transactional
    public Optional<EditWarAlert> processEdit(WikimediaEditEvent event) {
        // Skip if null or invalid
        if (event == null || event.getPageTitle() == null) {
            log.warn("Received null or invalid event");
            return Optional.empty();
        }

        // Get or create page window
        String pageKey = createPageKey(event.getPageTitle(), event.getWiki());

        PageEditWindow window = pageWindows.computeIfAbsent(
                pageKey,
                k -> new PageEditWindow(event.getPageTitle())
        );

        // Add edit to window
        window.addEdit(event);

        log.debug("Added edit to page {}: {} edits in window",
                pageKey, window.getEditCount());

        // Check for edit war
        if (window.isEditWar()) {
            log.info("EDIT WAR DETECTED on page: {}", pageKey);

            // Create alert
            EditWarAlert alert = createAlert(window, event.getWiki());

            // Save to database
            EditWarAlertEntity entity = alertMapper.toEntity(alert);
            EditWarAlertEntity savedEntity = alertRepository.save(entity);

            log.info("Alert saved to database with ID: {}", savedEntity.getId());

            return Optional.of(alert);
        }

        return Optional.empty();
    }

    /**
     * Create an EditWarAlert from a PageEditWindow
     */
    private EditWarAlert createAlert(PageEditWindow window, String wiki) {
        // Extract involved users
        List<String> users = window.getEdits().stream()
                .filter(event -> event.isHumanEdit() && event.isMainNamespace())
                .map(WikimediaEditEvent::getUsername)
                .distinct()
                .toList();

        // Get timestamps
        Long firstTimestamp = window.getEdits().stream()
                .map(WikimediaEditEvent::getTimestamp)
                .min(Long::compareTo)
                .orElse(System.currentTimeMillis());

        Long lastTimestamp = window.getEdits().stream()
                .map(WikimediaEditEvent::getTimestamp)
                .max(Long::compareTo)
                .orElse(System.currentTimeMillis());

        // Count conflicts
        int totalEdits = window.getEditCount();

        int conflictEdits = window.getConflictEditCount();

        // Ensure at least some conflicts exist
        if (conflictEdits == 0) {
            conflictEdits = 1;  // Avoid division by zero
        }

        // Build alert
        EditWarAlert alert = EditWarAlert.builder()
                .pageTitle(window.getPageTitle())
                .wiki(wiki)
                .involvedUsers(users)
                .userCount(users.size())
                .totalEdits(totalEdits)
                .conflictEdits(conflictEdits)
                .conflictRatio((double) conflictEdits / totalEdits)
                .detectedAt(Instant.now())
                .firstEditTimestamp(firstTimestamp)
                .lastEditTimestamp(lastTimestamp)
                .status(EditWarStatus.ACTIVE)
                .build();

        // Calculate severity
        alert.calculateSeverity();

        log.info("Created alert: {} - Severity: {} ({})",
                alert.getPageTitle(),
                alert.getSeverityScore(),
                alert.getSeverityLevel());

        return alert;

    }

    /**
     * Create unique key for page (includes wiki)
     */
    private String createPageKey(String pageTitle, String wiki) {
        return wiki + ":" + pageTitle;
    }

    /**
     * Clean up old windows periodically (prevents memory leaks)
     */
    public void cleanupOldWindows() {
        long now = System.currentTimeMillis();

        pageWindows.entrySet().removeIf(entry -> {
            PageEditWindow window = entry.getValue();

            // Remove if no edits in last 2 hours
            if (window.getEdits().isEmpty()) {
                return true;
            }
            Long lastEdit = window.getEdits().peekLast().getTimestamp();
            return (now - lastEdit) > 7200000L; // 2 hours
        });

        log.debug("Cleanup: {} pages being tracked", pageWindows.size());
    }

    /**
     * Get statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("trackedPages", pageWindows.size());
        stats.put("totalEdits", pageWindows.values().stream()
                .mapToInt(PageEditWindow::getEditCount)
                .sum());
        return stats;
    }
}
