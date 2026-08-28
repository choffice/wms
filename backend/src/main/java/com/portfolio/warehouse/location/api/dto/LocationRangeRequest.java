package com.portfolio.warehouse.location.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LocationRangeRequest(
    @NotNull @Min(0) Integer startNumber,
    @NotNull @Min(0) Integer endNumber,
    @Min(1) @Max(6) Integer width
) {
}
