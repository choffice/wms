package com.portfolio.warehouse.work.api.dto;

import com.portfolio.warehouse.work.domain.WorkType;

public record WorkTypeResponse(
    Long id,
    String name,
    String description,
    boolean active
) {
    public static WorkTypeResponse from(WorkType entity) {
        return new WorkTypeResponse(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.isActive()
        );
    }
}
