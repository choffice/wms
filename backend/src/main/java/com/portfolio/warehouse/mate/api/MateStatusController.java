package com.portfolio.warehouse.mate.api;

import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.mate.api.dto.MateResponse;
import com.portfolio.warehouse.mate.api.dto.MateStatusChangeRequest;
import com.portfolio.warehouse.mate.service.MateStatusService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class MateStatusController {

    private final MateStatusService service;
    private final CurrentUserService currentUserService;

    public MateStatusController(
        MateStatusService service,
        CurrentUserService currentUserService
    ) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/mate/status")
    public MateResponse currentStatus() {
        return MateResponse.from(currentUserService.mate());
    }

    @PatchMapping("/api/mate/status")
    public MateResponse changeMyStatus(
        @Valid @RequestBody MateStatusChangeRequest request
    ) {
        Long mateId = currentUserService.mate().getId();
        return service.changeStatus(mateId, request.status(), request.whereabouts());
    }

    @PatchMapping("/api/admin/mates/{mateId}/status")
    public MateResponse changeStatusByAdmin(
        @PathVariable Long mateId,
        @Valid @RequestBody MateStatusChangeRequest request
    ) {
        return service.changeStatus(mateId, request.status(), request.whereabouts());
    }
}
