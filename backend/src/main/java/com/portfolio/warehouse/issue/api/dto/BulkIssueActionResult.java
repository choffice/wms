package com.portfolio.warehouse.issue.api.dto;

import java.util.List;

public record BulkIssueActionResult(
    int processedCount,
    List<SpecialIssueResponse> issues
) {}
