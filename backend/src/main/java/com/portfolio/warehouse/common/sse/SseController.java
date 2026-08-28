package com.portfolio.warehouse.common.sse;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class SseController {

    private final SseHub hub;

    public SseController(SseHub hub) {
        this.hub = hub;
    }

    @GetMapping("/api/admin/events")
    public SseEmitter adminEvents() {
        return hub.subscribeAdmin();
    }

    @GetMapping("/api/mate/events")
    public SseEmitter mateEvents() {
        return hub.subscribeMate();
    }
}
