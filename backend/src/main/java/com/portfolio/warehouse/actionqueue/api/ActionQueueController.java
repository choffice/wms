package com.portfolio.warehouse.actionqueue.api;

import com.portfolio.warehouse.actionqueue.api.dto.ActionQueueResponse;
import com.portfolio.warehouse.actionqueue.service.ActionQueueService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/action-queue")
public class ActionQueueController {

    private final ActionQueueService service;

    public ActionQueueController(
        ActionQueueService service
    ) {
        this.service = service;
    }

    @GetMapping
    public ActionQueueResponse queue() {
        return service.queue();
    }
}
