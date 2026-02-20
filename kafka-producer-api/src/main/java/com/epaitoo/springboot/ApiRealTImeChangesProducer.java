package com.epaitoo.springboot;

import com.launchdarkly.eventsource.ConnectStrategy;
import com.launchdarkly.eventsource.EventSource;
import com.launchdarkly.eventsource.background.BackgroundEventHandler;
import com.launchdarkly.eventsource.background.BackgroundEventSource;
import okhttp3.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@Service
public class ApiRealTImeChangesProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiRealTImeChangesProducer.class);

    private KafkaTemplate<String, String> kafkaTemplate;

    public ApiRealTImeChangesProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    public void sendMessage() throws InterruptedException {
        LOGGER.info("STARTING TO CONNECT TO WIKIMEDIA...");

        String topic = "wikimedia-stream-api";
        String url = "https://stream.wikimedia.org/v2/stream/recentchange";


        BackgroundEventHandler backgroundEventHandler = new ApiRealTImeChangesHandler(kafkaTemplate, topic);
        LOGGER.info("Building EventSource for URL: {}", url);

        Headers headers = new Headers.Builder()
                .add("User-Agent", "SpringBootKafkaApp/1.0 (johnsmith@gmail.com) EditWarDetector")
                .build();

        ConnectStrategy connectStrategy = ConnectStrategy.http(URI.create(url))
                .headers(headers);


        EventSource.Builder eventSourceBuilder = new EventSource.Builder(connectStrategy);

        BackgroundEventSource.Builder backgroundEventSourceBuilder =
                new BackgroundEventSource.Builder(backgroundEventHandler, eventSourceBuilder);

        backgroundEventSourceBuilder.threadPriority(Thread.NORM_PRIORITY);

        BackgroundEventSource backgroundEventSource = backgroundEventSourceBuilder.build();
        LOGGER.info("Starting BackgroundEventSource...");
        backgroundEventSource.start();


        LOGGER.info("EventSource started! Listening for Wikimedia events for 8 minutes...");
        LOGGER.info("If you don't see events, check:");
        LOGGER.info("  1. Internet connection");
        LOGGER.info("  2. Wikimedia URL is accessible: {}", url);
        LOGGER.info("  3. Check ApiRealTImeChangesHandler for errors");

        TimeUnit.MINUTES.sleep(8);

    }
}
