package com.portfolio.warehouse.pda.api;

import com.portfolio.warehouse.pda.api.dto.*;
import com.portfolio.warehouse.pda.service.PdaAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/pdas")
public class PdaAdminController {

    private final PdaAdminService service;

    public PdaAdminController(PdaAdminService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PdaResponse create(@Valid @RequestBody PdaCreateRequest request) {
        return service.create(request.deviceNumber());
    }

    @GetMapping
    public List<PdaResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{deviceId}/usage-history")
    public List<PdaUsageResponse> usageHistory(@PathVariable Long deviceId) {
        return service.usageHistory(deviceId);
    }

    @PatchMapping("/{deviceId}/number")
    public PdaResponse changeNumber(
        @PathVariable Long deviceId,
        @Valid @RequestBody PdaNumberUpdateRequest request
    ) {
        return service.changeNumber(deviceId, request.deviceNumber());
    }

    @PatchMapping("/{deviceId}/status")
    public PdaResponse changeStatus(
        @PathVariable Long deviceId,
        @Valid @RequestBody PdaStatusUpdateRequest request
    ) {
        return service.changeStatus(deviceId, request.status());
    }

    @DeleteMapping("/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrRetire(@PathVariable Long deviceId) {
        service.deleteOrRetire(deviceId);
    }

    @PostMapping("/swap-numbers")
    public List<PdaResponse> swapNumbers(
        @Valid @RequestBody PdaSwapRequest request
    ) {
        return service.swapNumbers(
            request.firstDeviceId(),
            request.secondDeviceId()
        );
    }
}
