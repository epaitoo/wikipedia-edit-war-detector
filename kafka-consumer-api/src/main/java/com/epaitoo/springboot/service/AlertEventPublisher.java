package com.epaitoo.springboot.service;

import com.epaitoo.springboot.ApiEventListener;
import com.epaitoo.springboot.entity.EditWarAlert;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class AlertEventPublisher {
    private final ObjectMapper objectMapper;

    // Thread-safe list of listeners (supports multiple SSE connections)
    private final CopyOnWriteArrayList<ApiEventListener> listeners = new CopyOnWriteArrayList<>();

    public AlertEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Register an SSE listener to receive alerts
     *
     * @param listener The event listener to register
     */
    public void register(ApiEventListener listener) {
        listeners.add(listener);
        log.info("SSE listener registered. Total listeners: {}", listeners.size());
    }

    /**
     * Unregister an SSE listener
     *
     * @param listener The event listener to unregister
     */
    public void unregister(ApiEventListener listener) {
        listeners.remove(listener);
        log.info("SSE listener unregistered. Total listeners: {}", listeners.size());
    }

    /**
     * Publish an edit war alert to all connected SSE clients
     *
     * @param alert The edit war alert to broadcast
     */
    public void publishAlert(EditWarAlert alert) {
        if (listeners.isEmpty()) {
            log.debug("No SSE listeners connected, skipping broadcast for: {}", alert.getPageTitle());
            return;
        }

        try {
            // Format as JSON with type field for frontend
            String json = formatAlertAsJson(alert);

            // Broadcast to all listeners
            int successCount = 0;

            for (ApiEventListener listener : listeners) {
                try {
                    listener.onData(json);
                    successCount++;
                } catch (Exception e) {
                    log.warn("Failed to send alert to listener: {}", e.getMessage());
                }
            }

            log.info("Published alert to {} SSE client(s): {}", successCount, alert.getPageTitle());

        } catch (Exception e) {
            log.error("Failed to publish alert for page: {}", alert.getPageTitle(), e);
        }
    }

    /**
     * Publish a regular edit event (non-war)
     *
     * @param eventJson Raw JSON event
     */
    public void publishEdit(String eventJson) {
        if (listeners.isEmpty()) {
            return;
        }

        for (ApiEventListener listener : listeners) {
            try {
                listener.onData(eventJson);
            } catch (Exception e) {
                // Silently fail for regular edits
            }
        }
    }

    /**
     * Format alert as JSON with type wrapper for frontend parsing
     */
    private String formatAlertAsJson(EditWarAlert alert) throws Exception {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("type", "EDIT_WAR");
        wrapper.put("data", alert);

        return objectMapper.writeValueAsString(wrapper);
    }

    /**
     * Get count of connected listeners (for monitoring)
     */
    public int getListenerCount() {
        return listeners.size();
    }
}