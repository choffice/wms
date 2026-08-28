package com.portfolio.warehouse.location.api;

import com.portfolio.warehouse.location.api.dto.*;
import com.portfolio.warehouse.location.service.LocationAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/locations")
public class LocationAdminController {

    private final LocationAdminService service;

    public LocationAdminController(LocationAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<LocationResponse> findAll() {
        return service.findAll();
    }

    @PostMapping("/areas/range")
    @ResponseStatus(HttpStatus.CREATED)
    public List<LocationResponse> createAreaRange(
        @Valid @RequestBody LocationAreaRangeRequest request
    ) {
        return service.createAreaRange(request);
    }

    @PostMapping("/roots")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponse createRoot(@Valid @RequestBody LocationSegmentRequest request) {
        return service.createRoot(request.segment());
    }

    @PostMapping("/{parentId}/children")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponse addChild(
        @PathVariable Long parentId,
        @Valid @RequestBody LocationSegmentRequest request
    ) {
        return service.addChild(parentId, request.segment());
    }

    @PostMapping("/{referenceId}/siblings")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponse addSibling(
        @PathVariable Long referenceId,
        @Valid @RequestBody LocationSegmentRequest request
    ) {
        return service.addSibling(referenceId, request.segment());
    }

    @PostMapping("/{parentId}/children/range")
    @ResponseStatus(HttpStatus.CREATED)
    public List<LocationResponse> addNumericChildren(
        @PathVariable Long parentId,
        @Valid @RequestBody LocationRangeRequest request
    ) {
        return service.addNumericChildren(
            parentId,
            request.startNumber(),
            request.endNumber(),
            request.width()
        );
    }

    @GetMapping("/{parentId}/children")
    public List<LocationResponse> findChildren(@PathVariable Long parentId) {
        return service.findChildren(parentId);
    }


    @PatchMapping("/{locationId}/metadata")
    public LocationResponse updateMetadata(
        @PathVariable Long locationId,
        @Valid @RequestBody LocationMetadataRequest request
    ) {
        return service.updateMetadata(locationId, request);
    }

    @PostMapping("/{locationId}/deactivate")
    public LocationResponse deactivate(@PathVariable Long locationId) {
        return service.deactivate(locationId);
    }
}
