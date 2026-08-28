package com.portfolio.warehouse.handover.api.dto;

public record HandoverSummaryResponse(
    int pendingCount,
    int handoverCandidateCount,
    int assignedNotStartedCount,
    int pausedCount,
    int networkRecoveryCount,
    int offDutyCount,
    int mateBusyElsewhereCount
) {}
