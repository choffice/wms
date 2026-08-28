package com.portfolio.warehouse.pda.api.dto;

import jakarta.validation.constraints.NotNull;

public record PdaSwapRequest(
    @NotNull Long firstDeviceId,
    @NotNull Long secondDeviceId
) {
}
