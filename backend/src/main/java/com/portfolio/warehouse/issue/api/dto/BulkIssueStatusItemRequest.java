package com.portfolio.warehouse.issue.api.dto;

import jakarta.validation.constraints.NotNull;

public record BulkIssueStatusItemRequest(
    @NotNull Long issueId,
    @NotNull String expectedStatus
) {}
