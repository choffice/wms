package com.portfolio.warehouse.mate.api;

import com.portfolio.warehouse.issue.api.dto.IssueTypeResponse;
import com.portfolio.warehouse.location.api.dto.LocationResponse;
import com.portfolio.warehouse.mate.service.MateLookupService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mate/lookups")
public class MateLookupController {

    private final MateLookupService service;

    public MateLookupController(MateLookupService service) {
        this.service = service;
    }

    @GetMapping("/issue-types")
    public List<IssueTypeResponse> issueTypes() {
        return service.issueTypes();
    }

    @GetMapping("/locations")
    public List<LocationResponse> locations(
        @RequestParam(required = false) Long areaId
    ) {
        return service.locations(areaId);
    }
}
