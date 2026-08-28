package com.portfolio.warehouse.mate.api.dto;

import com.portfolio.warehouse.mate.domain.MateStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MateStatusChangeRequest(
    @NotNull MateStatus status,
    @Size(max = 100) String whereabouts
) {
}
