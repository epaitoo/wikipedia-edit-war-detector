package com.epaitoo.springboot.controller;

import com.epaitoo.springboot.entity.EditWarAlert;
import com.epaitoo.springboot.entity.WikimediaEditEvent;
import com.epaitoo.springboot.service.AlertEventPublisher;
import com.epaitoo.springboot.service.EditWarDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestDataController {
    private final EditWarDetectionService detectionService;
    private final AlertEventPublisher eventPublisher;

    @PostMapping("/simulate-edit-war")
    public Map<String, Object> simulateEditWar() {
        log.info("Starting edit war simulation...");

        long now = System.currentTimeMillis() / 1000;
        String testPage = "Donald_Trump_" + System.currentTimeMillis();

        Optional<EditWarAlert> alert = Optional.empty();

        // Simulate classic reverting pattern
        detectionService.processEdit(createTestEdit(testPage, "Alice", 5000, 5500, now));
        log.info("  Edit 1: Alice adds 500 bytes");

        detectionService.processEdit(createTestEdit(testPage, "Bob", 5500, 5000, now + 180));
        log.info("  Edit 2: Bob reverts (-500 bytes)");

        detectionService.processEdit(createTestEdit(testPage, "Alice", 5000, 5500, now + 360));
        log.info("  Edit 3: Alice reverts back (+500 bytes)");

        detectionService.processEdit(createTestEdit(testPage, "Bob", 5500, 5000, now + 540));
        log.info("  Edit 4: Bob reverts again (-500 bytes)");

        alert = detectionService.processEdit(createTestEdit(testPage, "Alice", 5000, 5500, now + 720));
        log.info("  Edit 5: Alice reverts one more time (+500 bytes)");

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("scenario", "Classic Reverting War");
        response.put("page", testPage);
        response.put("users", new String[]{"Alice", "Bob"});
        response.put("totalEdits", 5);

        if (alert.isPresent()) {
            EditWarAlert warAlert = alert.get();
            response.put("alertTriggered", true);
            response.put("severity", warAlert.getSeverityLevel());
            response.put("conflictRatio", String.format("%.0f%%", warAlert.getConflictRatio() * 100));
            log.info("ALERT TRIGGERED! Severity: {}", warAlert.getSeverityLevel());

            // Publish through event publisher
            eventPublisher.publishAlert(warAlert);

        } else {
            response.put("alertTriggered", false);
            log.warn("No alert triggered (check detection criteria)");
        }

        return response;
    }

    @PostMapping("/simulate-opposing-edits")
    public Map<String, Object> simulateOpposingEdits() {
        log.info("Starting opposing edits simulation...");

        long now = System.currentTimeMillis() / 1000;
        String testPage = "Climate_Change_" + System.currentTimeMillis();

        Optional<EditWarAlert> alert = Optional.empty();

        // User A adds content supporting one viewpoint
        detectionService.processEdit(createTestEdit(testPage, "UserA", 10000, 10800, now));

        // User B removes that content
        detectionService.processEdit(createTestEdit(testPage, "UserB", 10800, 10200, now + 200));

        // User C adds different content
        detectionService.processEdit(createTestEdit(testPage, "UserC", 10200, 10900, now + 400));

        // User A removes User C's additions
        detectionService.processEdit(createTestEdit(testPage, "UserA", 10900, 10300, now + 600));

        // User B adds more opposing content
        alert = detectionService.processEdit(createTestEdit(testPage, "UserB", 10300, 11000, now + 800));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("scenario", "Opposing Edits Pattern");
        response.put("page", testPage);
        response.put("users", new String[]{"UserA", "UserB", "UserC"});
        response.put("totalEdits", 5);
        response.put("alertTriggered", alert.isPresent());

        if (alert.isPresent()) {
            EditWarAlert warAlert = alert.get();
            response.put("severity", warAlert.getSeverityLevel());

            // Publish through event publisher
            eventPublisher.publishAlert(warAlert);
        }

        return response;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return detectionService.getStats();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Edit War Detection");
        return response;
    }

    // Helper method
    private WikimediaEditEvent createTestEdit(String page, String user,
                                              int oldLen, int newLen, long timestamp) {
        WikimediaEditEvent event = new WikimediaEditEvent();
        event.setPageTitle(page);
        event.setUsername(user);
        event.setLengthOld(oldLen);
        event.setLengthNew(newLen);
        event.setTimestamp(timestamp);
        event.setIsBot(false);
        event.setNamespace(0);  // Main namespace
        event.setType("edit");
        event.setWiki("en.wikipedia.org");
        return event;
    }
}
