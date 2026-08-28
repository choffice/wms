package com.portfolio.warehouse.log.api;

import com.portfolio.warehouse.log.api.dto.*;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.log.service.ActivityLogService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/activity-logs")
public class ActivityLogController {

    private final ActivityLogService service;

    public ActivityLogController(
        ActivityLogService service
    ) {
        this.service = service;
    }

    @GetMapping("/latest")
    public List<ActivityLogResponse> latest10() {
        return service.latest10();
    }

    @GetMapping
    public ActivityLogPageResponse search(
        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        )
        LocalDate from,

        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        )
        LocalDate to,

        @RequestParam(required = false)
        ActivityType type,

        @RequestParam(required = false)
        String actor,

        @RequestParam(required = false)
        String referenceType,

        @RequestParam(required = false)
        Long referenceId,

        @RequestParam(required = false)
        String keyword,

        @RequestParam(defaultValue = "0")
        int page,

        @RequestParam(defaultValue = "50")
        int size
    ) {
        return service.search(
            from,
            to,
            type,
            actor,
            referenceType,
            referenceId,
            keyword,
            page,
            size
        );
    }

    @GetMapping("/types")
    public ActivityType[] types() {
        return ActivityType.values();
    }

    @GetMapping("/reference-types")
    public List<String> referenceTypes() {
        return service.referenceTypes();
    }
}
