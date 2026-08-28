package com.portfolio.warehouse.report.api;

import com.portfolio.warehouse.report.api.dto.*;
import com.portfolio.warehouse.report.service.ReportService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/work-time")
    public List<WorkTimeStatResponse> workTimeStats(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to,

        @RequestParam(required = false) Long mateId,
        @RequestParam(required = false) Long workTypeId,
        @RequestParam(defaultValue = "false") boolean includeUncertain
    ) {
        return service.workTypeStats(from, to, mateId, workTypeId, includeUncertain);
    }

@GetMapping("/range")
public RangeReportResponse range(
    @RequestParam
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate from,

    @RequestParam
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate to
) {
    return service.range(from, to);
}

    @GetMapping("/shift-dates")
    public java.util.List<LocalDate> shiftDates(
        @RequestParam(defaultValue = "7")
        int limit
    ) {
        return service.recentShiftDates(limit);
    }

    @GetMapping("/shift/{shiftDate}")
    public ShiftReportResponse shift(
        @PathVariable
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate shiftDate
    ) {
        return service.shift(shiftDate);
    }

    @GetMapping("/daily/{date}")
    public DailyReportResponse daily(
        @PathVariable
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date
    ) {
        return service.daily(date);
    }
}
