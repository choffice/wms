package com.portfolio.warehouse.issue.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SpecialIssueCreateRequest(
    @NotNull Long issueTypeId,
    Long workAssignmentId,
    Long locationId,
    @Size(max = 80) String productCode,
    Integer quantity,
    Integer actualStock,
    Integer mmsStock,
    Integer expiryStock,
    boolean noStock,
    @NotBlank @Size(max = 1200) String comment
) {}
