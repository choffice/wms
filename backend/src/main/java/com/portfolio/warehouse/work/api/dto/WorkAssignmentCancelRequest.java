package com.portfolio.warehouse.work.api.dto;

import jakarta.validation.constraints.Size;

public record WorkAssignmentCancelRequest(
    @Size(max = 300) String reason
) {}
