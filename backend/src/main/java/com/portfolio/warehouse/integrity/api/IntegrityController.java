package com.portfolio.warehouse.integrity.api;

import com.portfolio.warehouse.integrity.api.dto.*;
import com.portfolio.warehouse.integrity.service.IntegrityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/integrity")
public class IntegrityController {

    private final IntegrityService service;

    public IntegrityController(
        IntegrityService service
    ) {
        this.service = service;
    }

    @GetMapping
    public IntegrityScanResponse scan() {
        return service.scan();
    }

    @PostMapping("/repair")
    public IntegrityRepairResult repair(
        @Valid
        @RequestBody
        IntegrityRepairRequest request
    ) {
        return service.repair(request);
    }

    @PostMapping("/repair-safe")
    public IntegrityRepairResult repairAllSafe() {
        return service.repairAllSafe();
    }
}
