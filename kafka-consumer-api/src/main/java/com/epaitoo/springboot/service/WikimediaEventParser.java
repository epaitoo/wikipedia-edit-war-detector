package com.epaitoo.springboot.service;

import com.epaitoo.springboot.entity.WikimediaEditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class WikimediaEventParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse JSON string into WikimediaEditEvent using Jackson auto-deserialization
     */
    public WikimediaEditEvent parse(String jsonString) {
        try {
            WikimediaEditEvent event = objectMapper.readValue(jsonString, WikimediaEditEvent.class);

            // Process nested fields (flatten structure)
            event.processNestedFields();

            // Validate: only process edit events with required data
            if (!event.isEditType() || event.getPageTitle() == null || event.getUsername() == null) {
                log.debug("Skipping non-edit event or invalid data: type={}, title={}",
                        event.getType(), event.getPageTitle());
                return null;
            }

            // Set timestamp default if missing
            if (event.getTimestamp() == null) {
                event.setTimestamp(System.currentTimeMillis() / 1000);
            }

            log.debug("Successfully parsed event for page: {}", event.getPageTitle());
            return event;

        } catch (Exception e) {
            log.error("Failed to parse Wikimedia event: {}", e.getMessage());
            log.debug("Raw JSON: {}", jsonString.substring(0, Math.min(200, jsonString.length())));
            return null;
        }
    }
}
