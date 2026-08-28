package com.portfolio.warehouse.mate.api;

import com.portfolio.warehouse.mate.api.dto.MateCreateRequest;
import com.portfolio.warehouse.mate.api.dto.MateNicknameUpdateRequest;
import com.portfolio.warehouse.mate.api.dto.MateResponse;
import com.portfolio.warehouse.mate.service.MateAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/mates")
public class MateAdminController {

    private final MateAdminService service;

    public MateAdminController(MateAdminService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MateResponse create(@Valid @RequestBody MateCreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<MateResponse> findAll() {
        return service.findAll();
    }

    @PatchMapping("/{mateId}/nickname")
    public MateResponse changeNickname(
        @PathVariable Long mateId,
        @Valid @RequestBody MateNicknameUpdateRequest request
    ) {
        return service.changeNickname(mateId, request.nickname());
    }

    @PostMapping("/{mateId}/deactivate")
    public MateResponse deactivate(@PathVariable Long mateId) {
        return service.deactivate(mateId);
    }

    @PostMapping("/{mateId}/reactivate")
    public MateResponse reactivate(@PathVariable Long mateId) {
        return service.reactivate(mateId);
    }
}
