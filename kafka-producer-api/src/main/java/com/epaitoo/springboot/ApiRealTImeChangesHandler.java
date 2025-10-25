package com.epaitoo.springboot;

import com.launchdarkly.eventsource.MessageEvent;
import com.launchdarkly.eventsource.background.BackgroundEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

public class ApiRealTImeChangesHandler implements BackgroundEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiRealTImeChangesHandler.class);

    private KafkaTemplate<String, String> kafkaTemplate;
    private String topic;

    public ApiRealTImeChangesHandler(KafkaTemplate<String, String> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }
    @Override
    public void onOpen() throws Exception {
        LOGGER.info("✅ Connection to Wikimedia OPENED!");
    }

    @Override
    public void onClosed() throws Exception {
        LOGGER.info("❌ Connection to Wikimedia CLOSED");
    }

    @Override
    public void onMessage(String s, MessageEvent messageEvent) throws Exception {
        LOGGER.info("📨 Received event: {}", messageEvent.getData().substring(0, Math.min(100, messageEvent.getData().length())));


        kafkaTemplate.send(topic, messageEvent.getData());
    }

    @Override
    public void onComment(String s) throws Exception {

    }

    @Override
    public void onError(Throwable throwable) {
        LOGGER.error("❌❌❌ ERROR in EventSource: {}", throwable.getMessage(), throwable);
    }
}
