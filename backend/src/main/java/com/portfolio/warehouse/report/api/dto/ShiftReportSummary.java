package com.portfolio.warehouse.report.api.dto;

public record ShiftReportSummary(
    long actualWorkSeconds,
    long sessionCount,
    long openSessionCount,
    long uncertainSessionCount,
    long assignmentCount,
    long mateCount,
    long issueCount,
    long pdaUsageCount,
    long overnightSessionCount
) {}
