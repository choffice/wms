package com.portfolio.warehouse.issue.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BulkIssueResponsibleRequest(
    @Valid
    @NotEmpty
    @Size(max = 50)
    List<BulkIssueResponsibleItemRequest> items,
    Long toMateId,
    @Size(max = 300)
    String reason
) {}
