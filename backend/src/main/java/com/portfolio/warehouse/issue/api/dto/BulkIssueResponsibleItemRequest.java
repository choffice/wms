package com.portfolio.warehouse.issue.api.dto;

import jakarta.validation.constraints.NotNull;

public record BulkIssueResponsibleItemRequest(
    @NotNull Long issueId,
    Long expectedResponsibleMateId
) {}
