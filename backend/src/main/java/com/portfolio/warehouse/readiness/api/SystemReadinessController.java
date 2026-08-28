package com.portfolio.warehouse.readiness.api;

import com.portfolio.warehouse.readiness.api.dto.SystemReadinessResponse;
import com.portfolio.warehouse.readiness.service.SystemReadinessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/system-readiness")
public class SystemReadinessController {

    private final SystemReadinessService service;

    public SystemReadinessController(
        SystemReadinessService service
    ) {
        this.service = service;
    }

    @GetMapping
    public SystemReadinessResponse readiness() {
        return service.readiness();
    }
}
