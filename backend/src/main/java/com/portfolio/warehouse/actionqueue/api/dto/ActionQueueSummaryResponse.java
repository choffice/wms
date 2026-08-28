package com.portfolio.warehouse.actionqueue.api.dto;

public record ActionQueueSummaryResponse(
    int totalCount,
    int blockerCount,
    int attentionCount,
    int handoverCount,
    int issueCount
) {}
