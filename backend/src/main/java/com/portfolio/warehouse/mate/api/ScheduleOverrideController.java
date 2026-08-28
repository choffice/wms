package com.portfolio.warehouse.mate.api;

import com.portfolio.warehouse.mate.api.dto.*;
import com.portfolio.warehouse.mate.service.ScheduleOverrideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class ScheduleOverrideController {

    private final ScheduleOverrideService service;

    public ScheduleOverrideController(ScheduleOverrideService service) {
        this.service = service;
    }

    @PostMapping("/api/admin/mates/{mateId}/schedule-overrides")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleOverrideResponse createPeriod(
        @PathVariable Long mateId,
        @Valid @RequestBody ScheduleOverrideRequest request
    ) {
        return service.createPeriod(mateId, request);
    }

    @PostMapping("/api/admin/mates/{mateId}/extension")
    public ScheduleOverrideResponse extendByAdmin(@PathVariable Long mateId) {
        return service.extendTodayForAdmin(mateId);
    }

@DeleteMapping("/api/admin/mates/{mateId}/extension")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void cancelExtensionByAdmin(@PathVariable Long mateId) {
    service.cancelExtensionTodayForAdmin(mateId);
}

    @PostMapping("/api/mate/extension")
    public ScheduleOverrideResponse extendToday() {
        return service.extendToday();
    }

    @DeleteMapping("/api/mate/extension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelExtension() {
        service.cancelExtensionToday();
    }
}
