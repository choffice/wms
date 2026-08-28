package com.portfolio.warehouse.work.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUndoProgressCorrectionRequest(
    @NotNull Long expectedLatestProgressId,
    @NotNull Long expectedCurrentLocationId,
    @Size(max = 300) String reason
) {}
