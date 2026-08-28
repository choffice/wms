package com.portfolio.warehouse.work.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkProgressRequest(
    Long expectedCurrentLocationId,
    @NotNull Long lastCompletedLocationId,
    @Size(max = 300) String reason
) {}
