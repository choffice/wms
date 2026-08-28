package com.portfolio.warehouse.work.api.dto;

import jakarta.validation.constraints.NotNull;

public record WorkAssignmentCreateRequest(
    @NotNull Long workTypeId,
    @NotNull Long areaLocationId,
    @NotNull Long startLocationId,
    @NotNull Long mateId
) {}
