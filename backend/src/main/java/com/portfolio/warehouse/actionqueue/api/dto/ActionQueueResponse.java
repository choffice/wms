package com.portfolio.warehouse.actionqueue.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ActionQueueResponse(
    LocalDateTime generatedAt,
    ActionQueueSummaryResponse summary,
    List<ActionQueueItemResponse> items
) {}
