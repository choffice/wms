package com.portfolio.warehouse.work.api.dto;

import jakarta.validation.constraints.Size;

public record WorkCompleteRequest(
    Long expectedCurrentLocationId,
    Long lastCompletedLocationId,
    @Size(max = 300) String correctionReason
) {}
