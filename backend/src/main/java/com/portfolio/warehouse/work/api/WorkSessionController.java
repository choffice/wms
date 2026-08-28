package com.portfolio.warehouse.work.api;

import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.work.service.WorkSessionReliabilityService;
import com.portfolio.warehouse.work.api.dto.WorkSessionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mate/work-sessions")
public class WorkSessionController {

    private final WorkSessionReliabilityService reliabilityService;
    private final CurrentUserService currentUserService;

    public WorkSessionController(
        WorkSessionReliabilityService reliabilityService,
        CurrentUserService currentUserService
    ) {
        this.reliabilityService = reliabilityService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/current")
    public WorkSessionResponse current() {
        return reliabilityService.currentOpenSessionResponse(
            currentUserService.mate().getId()
        );
    }

    @PostMapping("/{sessionId}/heartbeat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void heartbeat(@PathVariable Long sessionId) {
        reliabilityService.heartbeat(sessionId, currentUserService.mate().getId());
    }
}
