package com.portfolio.warehouse.operations.api.dto;

public record OperationsSummaryResponse(
    int activeMateCount,
    int availableMateCount,
    int workingMateCount,
    int breakMateCount,
    int awayMateCount,
    int activeSessionCount,
    int uncertainSessionCount,
    int pdaInUseCount,
    int pdaAttentionCount,
    int unconfirmedIssueCount,
    int unassignedOpenIssueCount,
    int attentionMateCount
) {}
