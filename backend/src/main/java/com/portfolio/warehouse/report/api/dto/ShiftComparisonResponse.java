package com.portfolio.warehouse.report.api.dto;

import java.time.LocalDate;

public record ShiftComparisonResponse(
    LocalDate previousShiftDate,
    long previousWorkSeconds,
    long workSecondsDelta,
    long previousIssueCount,
    long issueCountDelta
) {}
