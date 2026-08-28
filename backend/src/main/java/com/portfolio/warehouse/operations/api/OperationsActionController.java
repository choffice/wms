package com.portfolio.warehouse.operations.api;

import com.portfolio.warehouse.operations.service.OperationsActionService;
import com.portfolio.warehouse.pda.api.dto.PdaUsageResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/operations")
public class OperationsActionController {

    private final OperationsActionService service;

    public OperationsActionController(
        OperationsActionService service
    ) {
        this.service = service;
    }

    @PostMapping("/pda-usages/{usageId}/release")
    public PdaUsageResponse forceReleasePda(
        @PathVariable Long usageId
    ) {
        return service.forceReleasePda(usageId);
    }
}
