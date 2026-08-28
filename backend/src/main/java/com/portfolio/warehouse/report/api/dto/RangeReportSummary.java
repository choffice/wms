package com.portfolio.warehouse.report.api.dto;

public record RangeReportSummary(
    long sessionCount,
    long assignmentCount,
    long mateCount,
    long normalSeconds,
    long uncertainSeconds,
    long issueCount,
    long pdaUsageCount
) {}
