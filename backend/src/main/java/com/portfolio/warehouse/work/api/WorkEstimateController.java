package com.portfolio.warehouse.work.api;

import com.portfolio.warehouse.work.api.dto.WorkEstimateResponse;
import com.portfolio.warehouse.work.service.WorkEstimateService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/work-estimates")
public class WorkEstimateController {

    private final WorkEstimateService service;

    public WorkEstimateController(WorkEstimateService service) {
        this.service = service;
    }

    @GetMapping
    public WorkEstimateResponse estimate(
        @RequestParam Long areaId,
        @RequestParam Long workTypeId,
        @RequestParam(required = false) Long startLocationId
    ) {
        return service.estimate(
            areaId,
            workTypeId,
            startLocationId
        );
    }
}
