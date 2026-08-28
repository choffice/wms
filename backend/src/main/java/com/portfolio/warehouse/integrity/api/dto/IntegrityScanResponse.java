package com.portfolio.warehouse.integrity.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record IntegrityScanResponse(
    LocalDateTime generatedAt,
    IntegritySummaryResponse summary,
    List<IntegrityIssueResponse> issues
) {}
