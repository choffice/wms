package com.portfolio.warehouse.log.api.dto;

import com.portfolio.warehouse.log.domain.ActivityLog;
import java.time.LocalDateTime;

public record ActivityLogResponse(
    Long id,
    String type,
    String actor,
    String target,
    String message,
    String referenceType,
    Long referenceId,
    LocalDateTime createdAt
) {
    public static ActivityLogResponse from(ActivityLog entity) {
        return new ActivityLogResponse(
            entity.getId(),
            entity.getType().name(),
            entity.getActorAccount() == null ? null : entity.getActorAccount().getLoginId(),
            entity.getTargetLabel(),
            entity.getMessage(),
            entity.getReferenceType(),
            entity.getReferenceId(),
            entity.getCreatedAt()
        );
    }
}
