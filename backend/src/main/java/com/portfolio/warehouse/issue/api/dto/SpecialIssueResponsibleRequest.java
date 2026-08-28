package com.portfolio.warehouse.issue.api.dto;

import jakarta.validation.constraints.Size;

public record SpecialIssueResponsibleRequest(
    Long mateId,
    Long expectedResponsibleMateId,
    @Size(max = 300) String reason
) {}
