package com.portfolio.warehouse.issue.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IssueTypeRequest(
    @NotBlank @Size(max = 60) String name,
    boolean requireLocation,
    boolean requireProductCode,
    boolean requireQuantity
) {}
