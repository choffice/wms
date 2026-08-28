package com.portfolio.warehouse.work.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkTypeRequest(
    @NotBlank @Size(max = 60) String name,
    @Size(max = 300) String description
) {}
