package com.portfolio.warehouse.common.sse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseHub {

    private final List<SseEmitter> adminEmitters = new CopyOnWriteArrayList<>();
    private final List<SseEmitter> mateEmitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribeAdmin() {
        return subscribe(adminEmitters);
    }

    public SseEmitter subscribeMate() {
        return subscribe(mateEmitters);
    }

    public void publishAdmin(String eventName, Object data) {
        publish(adminEmitters, eventName, data);
    }

    public void publishMate(String eventName, Object data) {
        publish(mateEmitters, eventName, data);
    }

    private SseEmitter subscribe(List<SseEmitter> bucket) {
        SseEmitter emitter = new SseEmitter(30L * 60L * 1000L);
        bucket.add(emitter);

        emitter.onCompletion(() -> bucket.remove(emitter));
        emitter.onTimeout(() -> bucket.remove(emitter));
        emitter.onError(error -> bucket.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            bucket.remove(emitter);
        }

        return emitter;
    }

    private void publish(List<SseEmitter> bucket, String eventName, Object data) {
        for (SseEmitter emitter : bucket) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                bucket.remove(emitter);
            }
        }
    }
}
