package com.portfolio.warehouse.shiftclose.api.dto;

public record ShiftCloseSummaryResponse(
    int blockerCount,
    int warningCount,
    int okCount,
    boolean readyForHandoverReview
) {}
