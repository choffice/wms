package com.portfolio.warehouse.issue.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkIssueStatusRequest(
    @Valid
    @NotEmpty
    @Size(max = 50)
    List<BulkIssueStatusItemRequest> items
) {}
