package com.portfolio.warehouse.pda.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PdaNumberUpdateRequest(
    @NotNull @Min(1) Integer deviceNumber
) {}
