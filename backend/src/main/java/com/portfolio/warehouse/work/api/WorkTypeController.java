package com.portfolio.warehouse.work.api;

import com.portfolio.warehouse.work.api.dto.*;
import com.portfolio.warehouse.work.service.WorkTypeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/work-types")
public class WorkTypeController {

    private final WorkTypeService service;

    public WorkTypeController(WorkTypeService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkTypeResponse create(@Valid @RequestBody WorkTypeRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<WorkTypeResponse> findAll() {
        return service.findAll();
    }

    @PutMapping("/{id}")
    public WorkTypeResponse update(@PathVariable Long id, @Valid @RequestBody WorkTypeRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/deactivate")
    public WorkTypeResponse deactivate(@PathVariable Long id) {
        return service.deactivate(id);
    }
}
