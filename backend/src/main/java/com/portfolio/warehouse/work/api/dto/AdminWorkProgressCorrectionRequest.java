package com.portfolio.warehouse.work.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminWorkProgressCorrectionRequest(
    Long expectedCurrentLocationId,
    @NotNull Long correctedLocationId,
    @Size(max = 300) String reason
) {}
