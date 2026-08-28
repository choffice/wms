package com.portfolio.warehouse.work.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkAssignmentTradeRequest(
    @NotNull Long toMateId,
    Long expectedCurrentMateId,
    @Size(max = 300) String reason
) {}
