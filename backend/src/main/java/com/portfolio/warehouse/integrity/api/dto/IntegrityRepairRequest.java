package com.portfolio.warehouse.integrity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IntegrityRepairRequest(
    @NotBlank String action,
    @NotNull Long entityId
) {}
