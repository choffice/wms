package com.portfolio.warehouse.operations.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OperationsBoardResponse(
    LocalDateTime generatedAt,
    OperationsSummaryResponse summary,
    List<MateOperationRow> mates
) {}
