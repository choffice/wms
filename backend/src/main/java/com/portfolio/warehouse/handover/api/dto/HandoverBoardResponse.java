package com.portfolio.warehouse.handover.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HandoverBoardResponse(
    LocalDateTime generatedAt,
    HandoverSummaryResponse summary,
    List<HandoverRowResponse> rows
) {}
