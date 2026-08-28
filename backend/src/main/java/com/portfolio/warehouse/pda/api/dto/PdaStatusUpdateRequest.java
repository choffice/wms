package com.portfolio.warehouse.pda.api.dto;

import com.portfolio.warehouse.pda.domain.PdaStatus;
import jakarta.validation.constraints.NotNull;

public record PdaStatusUpdateRequest(
    @NotNull PdaStatus status
) {
}
