package com.portfolio.warehouse.issue.api.dto;

import com.portfolio.warehouse.issue.domain.IssueType;

public record IssueTypeResponse(
    Long id,
    String name,
    boolean requireLocation,
    boolean requireProductCode,
    boolean requireQuantity,
    boolean active
) {
    public static IssueTypeResponse from(IssueType entity) {
        return new IssueTypeResponse(
            entity.getId(),
            entity.getName(),
            entity.isRequireLocation(),
            entity.isRequireProductCode(),
            entity.isRequireQuantity(),
            entity.isActive()
        );
    }
}
