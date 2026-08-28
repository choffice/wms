package com.portfolio.warehouse.pda.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PdaCreateRequest(
    @NotNull
    @Min(value = 1, message = "PDA 번호는 1 이상이어야 합니다.")
    Integer deviceNumber
) {
}
