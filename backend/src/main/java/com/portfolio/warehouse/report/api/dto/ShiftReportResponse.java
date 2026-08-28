package com.portfolio.warehouse.report.api.dto;

import java.time.LocalDate;
import java.util.List;

public record ShiftReportResponse(
    LocalDate shiftDate,
    ShiftReportSummary summary,
    ShiftComparisonResponse comparison,
    List<DailyWorkRow> works,
    List<DailyPdaRow> pdaUsages,
    List<DailyIssueRow> issues
) {}
