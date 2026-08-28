package com.portfolio.warehouse.handover.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BulkHandoverItemRequest(
    @NotNull Long assignmentId,
    @NotNull Long expectedCurrentMateId,
    @NotNull Long toMateId,
    @Size(max = 300) String reason
) {}
