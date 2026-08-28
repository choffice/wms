package com.portfolio.warehouse.handover.api;

import com.portfolio.warehouse.handover.api.dto.HandoverBoardResponse;
import com.portfolio.warehouse.handover.service.HandoverService;
import com.portfolio.warehouse.handover.service.HandoverActionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/handover")
public class HandoverController {

    private final HandoverService service;
    private final HandoverActionService actionService;

    public HandoverController(
        HandoverService service,
        HandoverActionService actionService
    ) {
        this.service = service;
        this.actionService = actionService;
    }

    @GetMapping
    public HandoverBoardResponse board() {
        return service.board();
    }

    @PostMapping("/bulk-transfer")
    public BulkHandoverResultResponse bulkTransfer(
        @Valid @RequestBody BulkHandoverRequest request
    ) {
        return actionService.bulkTransfer(request);
    }
}
