package com.epaitoo.springboot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchdarkly.eventsource.MessageEvent;
import com.launchdarkly.eventsource.background.BackgroundEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Set;

public class ApiRealTImeChangesHandler implements BackgroundEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiRealTImeChangesHandler.class);

    // Only process English Wikipedia
    private static final Set<String> MONITORED_WIKIS = Set.of(
            "en.wikipedia.org"
    );

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiRealTImeChangesHandler(KafkaTemplate<String, String> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void onOpen() throws Exception {
        LOGGER.info("Connection to Wikimedia OPENED!");
    }

    @Override
    public void onClosed() throws Exception {
        LOGGER.info("Connection to Wikimedia CLOSED");
    }

    @Override
    public void onMessage(String s, MessageEvent messageEvent) throws Exception {
        String data = messageEvent.getData();

        try {
            JsonNode json = objectMapper.readTree(data);

            // Get the wiki/server_name
            String wiki = null;
            if (json.has("server_name")) {
                wiki = json.get("server_name").asText();
            } else if (json.has("meta") && json.get("meta").has("domain")) {
                wiki = json.get("meta").get("domain").asText();
            }

            // Skip if not a monitored wiki
            if (wiki == null || !MONITORED_WIKIS.contains(wiki)) {
                return;
            }

            // Only process "edit" type events
            String type = json.has("type") ? json.get("type").asText() : "";
            if (!"edit".equals(type)) {
                return;
            }

            // Skip bot edits
            boolean isBot = json.has("bot") && json.get("bot").asBoolean();
            if (isBot) {
                return;
            }

            LOGGER.info("Processing edit: {}",
                    json.has("title") ? json.get("title").asText() : "unknown");

            kafkaTemplate.send(topic, data);

        } catch (Exception e) {
            LOGGER.debug("Failed to parse event: {}", e.getMessage());
        }
    }

    @Override
    public void onComment(String s) throws Exception {
    }

    @Override
    public void onError(Throwable throwable) {
        LOGGER.error("ERROR in EventSource: {}", throwable.getMessage(), throwable);
    }
}