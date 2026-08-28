package com.portfolio.warehouse.work.api;

import com.portfolio.warehouse.work.api.dto.*;
import com.portfolio.warehouse.work.service.WorkAssignmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mate/work-assignments")
public class MateWorkAssignmentController {

    private final WorkAssignmentService service;

    public MateWorkAssignmentController(WorkAssignmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<WorkAssignmentResponse> myAssignments() {
        return service.myAssignments();
    }

    @PostMapping("/{assignmentId}/start")
    public WorkSessionResponse start(@PathVariable Long assignmentId) {
        return service.start(assignmentId);
    }

    @PostMapping("/{assignmentId}/progress")
    public WorkProgressResponse progress(
        @PathVariable Long assignmentId,
        @Valid @RequestBody WorkProgressRequest request
    ) {
        return service.progress(assignmentId, request);
    }

    @PostMapping("/{assignmentId}/pause")
    public WorkSessionResponse pause(
        @PathVariable Long assignmentId,
        @RequestBody(required = false) WorkPauseRequest request
    ) {
        return service.pause(assignmentId, request);
    }

    @PostMapping("/{assignmentId}/resume")
    public WorkSessionResponse resume(@PathVariable Long assignmentId) {
        return service.resume(assignmentId);
    }

    @PostMapping("/{assignmentId}/complete")
    public WorkAssignmentResponse complete(
        @PathVariable Long assignmentId,
        @RequestBody(required = false) WorkCompleteRequest request
    ) {
        return service.complete(assignmentId, request);
    }
}
