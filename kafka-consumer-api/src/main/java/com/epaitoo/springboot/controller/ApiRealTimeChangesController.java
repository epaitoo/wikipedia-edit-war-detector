package com.epaitoo.springboot.controller;

import com.epaitoo.springboot.ApiEventListener;
import com.epaitoo.springboot.ApiRealTImeChangesConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ApiRealTimeChangesController {

    @Autowired
    private ApiRealTImeChangesConsumer apiRealTImeChangesConsumer;

    private Flux<String> bridge;

    public ApiRealTimeChangesController() {
        this.bridge = createBridge().publish().autoConnect().cache(10).log();
    }

    @GetMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> getStreamEvents() {
        return bridge;
    }

    private Flux<String> createBridge() {
        return Flux.create(sink -> {
            apiRealTImeChangesConsumer.register(new ApiEventListener() {
                @Override
                public void onData(String event) {
                    sink.next(event);
                }

                @Override
                public void processComplete() {
                    sink.complete();
                }
            });
        });
    }

}
