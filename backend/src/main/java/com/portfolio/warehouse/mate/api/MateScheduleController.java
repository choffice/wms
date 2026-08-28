package com.portfolio.warehouse.mate.api;

import com.portfolio.warehouse.mate.api.dto.ScheduleItemRequest;
import com.portfolio.warehouse.mate.api.dto.ScheduleResponse;
import com.portfolio.warehouse.mate.service.MateScheduleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/mates/{mateId}/schedules")
public class MateScheduleController {

    private final MateScheduleService service;

    public MateScheduleController(MateScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public List<ScheduleResponse> findAll(@PathVariable Long mateId) {
        return service.findAll(mateId);
    }

    @PutMapping
    public List<ScheduleResponse> replace(
        @PathVariable Long mateId,
        @Valid @RequestBody List<ScheduleItemRequest> requests
    ) {
        return service.replace(mateId, requests);
    }
}
