package com.portfolio.warehouse.work.api;

import com.portfolio.warehouse.work.api.dto.*;
import com.portfolio.warehouse.work.service.WorkAssignmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/work-assignments")
public class AdminWorkAssignmentController {

    private final WorkAssignmentService service;

    public AdminWorkAssignmentController(WorkAssignmentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkAssignmentResponse assign(@Valid @RequestBody WorkAssignmentCreateRequest request) {
        return service.assign(request);
    }

    @GetMapping
    public List<WorkAssignmentResponse> list() {
        return service.adminList();
    }

    @PostMapping("/{assignmentId}/trade")
    public WorkAssignmentResponse trade(
        @PathVariable Long assignmentId,
        @Valid @RequestBody WorkAssignmentTradeRequest request
    ) {
        return service.trade(assignmentId, request);
    }

@PostMapping("/{assignmentId}/cancel")
public WorkAssignmentResponse cancel(
    @PathVariable Long assignmentId,
    @RequestBody(required = false) WorkAssignmentCancelRequest request
) {
    return service.cancel(assignmentId, request);
}

    @PostMapping("/{assignmentId}/progress-correction")
    public WorkProgressResponse correctProgress(
        @PathVariable Long assignmentId,
        @Valid @RequestBody
        AdminWorkProgressCorrectionRequest request
    ) {
        return service.adminCorrectProgress(
            assignmentId,
            request
        );
    }

    @PostMapping("/{assignmentId}/progress-correction/undo-latest")
    public WorkProgressResponse undoLatestProgressCorrection(
        @PathVariable Long assignmentId,
        @Valid @RequestBody
        AdminUndoProgressCorrectionRequest request
    ) {
        return service.undoLatestProgressCorrection(
            assignmentId,
            request
        );
    }

    @GetMapping("/{assignmentId}/progress-history")
    public List<WorkProgressResponse> progressHistory(@PathVariable Long assignmentId) {
        return service.progressHistory(assignmentId);
    }

@GetMapping("/{assignmentId}/assignment-history")
public List<WorkAssignmentHistoryResponse> assignmentHistory(
    @PathVariable Long assignmentId
) {
    return service.assignmentHistory(assignmentId);
}

    @GetMapping("/{assignmentId}/session-history")
    public List<WorkSessionResponse> sessionHistory(@PathVariable Long assignmentId) {
        return service.sessionHistory(assignmentId);
    }
}
