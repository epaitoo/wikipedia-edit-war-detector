package com.epaitoo.springboot;

import com.epaitoo.springboot.entity.EditWarAlert;
import com.epaitoo.springboot.entity.WikimediaEditEvent;
import com.epaitoo.springboot.service.AlertEventPublisher;
import com.epaitoo.springboot.service.EditWarDetectionService;
import com.epaitoo.springboot.service.WikimediaEventParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApiRealTImeChangesConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiRealTImeChangesConsumer.class);

    private final WikimediaEventParser parser;
    private final EditWarDetectionService detectionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AlertEventPublisher eventPublisher;

    private ApiEventListener apiEventListener;

    public void register(ApiEventListener apiEventListener) {
        this.apiEventListener = apiEventListener;
        eventPublisher.register(apiEventListener);
    }

    public void onEvent(String event) {
        if (apiEventListener != null) apiEventListener.onData(event);
    }

    public void onComplete() {
        if (apiEventListener != null) apiEventListener.processComplete();
    }

    @KafkaListener(topics = "wikimedia-stream-api", groupId = "myGroup")
    public void consume(String eventMessage) {
        // Parse JSON into WikimediaEditEvent
        WikimediaEditEvent event = parser.parse(eventMessage);

        if (event == null) {
            LOGGER.debug("Skipped invalid or non-edit event");
            return;
        }

        LOGGER.info("Processing edit on page: {} by user: {}",
                event.getPageTitle(), event.getUsername());

        // Detect edit wars
        Optional<EditWarAlert> alertOpt = detectionService.processEdit(event);

        if (alertOpt.isPresent()) {
            EditWarAlert alert = alertOpt.get();
            LOGGER.warn("\uD83D\uDEA8\uD83D\uDEA8\uD83D\uDEA8🚨🚨🚨 EDIT WAR DETECTED \uD83D\uDEA8\uD83D\uDEA8\uD83D\uDEA8🚨🚨🚨");
            LOGGER.warn("Page: {}", alert.getPageTitle());
            LOGGER.warn("Users: {}", alert.getInvolvedUsers());
            LOGGER.warn("Severity: {} ({})", alert.getSeverityScore(), alert.getSeverityLevel());
            LOGGER.warn("Edits: {} ({}% conflict)",
                    alert.getTotalEdits(),
                    (int)(alert.getConflictRatio() * 100));

            // Publish through event publisher
            eventPublisher.publishAlert(alert);

        } else {
            // Stream normal event to frontend
            onEvent(eventMessage);
        }
    }

    /**
     * Format alert as JSON string for streaming
     */
    private String formatAlert(EditWarAlert alert) {
        return String.format(
                "{\"type\":\"EDIT_WAR\",\"page\":\"%s\",\"users\":%s,\"severity\":\"%s\",\"edits\":%d}",
                alert.getPageTitle(),
                alert.getInvolvedUsers().toString(),
                alert.getSeverityLevel(),
                alert.getTotalEdits()
        );
    }

    /**
     * Format alert as JSON with type field for frontend
     */
    private String formatAlertAsJson(EditWarAlert alert) {
        try {
            // Create a wrapper object with type field
            Map<String, Object> wrapper = new HashMap<>();
            wrapper.put("type", "EDIT_WAR");
            wrapper.put("data", alert);

            return objectMapper.writeValueAsString(wrapper);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize alert", e);
            return "{\"type\":\"ERROR\"}";
        }
    }
}