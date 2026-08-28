package com.portfolio.warehouse.report.api.dto;

public record WorkTimeStatResponse(
    String workType,
    long sessionCount,
    long totalSeconds,
    long averageSeconds
) {}
