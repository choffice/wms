package com.portfolio.warehouse.work.api.dto;

import com.portfolio.warehouse.work.domain.WorkAssignment;
import java.time.LocalDateTime;

public record WorkAssignmentResponse(
    Long id,
    Long workTypeId,
    String workTypeName,
    Long areaLocationId,
    String areaLocation,
    Long startLocationId,
    String startLocation,
    Long currentMateId,
    String currentMateNickname,
    String assignedBy,
    LocalDateTime assignedAt,
    Long currentLastCompletedLocationId,
    String currentLastCompletedLocation,
    String status,
    LocalDateTime completedAt
) {
    public static WorkAssignmentResponse from(WorkAssignment entity) {
        return new WorkAssignmentResponse(
            entity.getId(),
            entity.getWorkType().getId(),
            entity.getWorkType().getName(),
            entity.getAreaLocation().getId(),
            entity.getAreaLocation().getFullCode(),
            entity.getStartLocation().getId(),
            entity.getStartLocation().getFullCode(),
            entity.getCurrentMate().getId(),
            entity.getCurrentMate().getNickname(),
            entity.getAssignedBy().getLoginId(),
            entity.getAssignedAt(),
            entity.getCurrentLastCompletedLocation() == null
                ? null
                : entity.getCurrentLastCompletedLocation().getId(),
            entity.getCurrentLastCompletedLocation() == null
                ? null
                : entity.getCurrentLastCompletedLocation().getFullCode(),
            entity.getStatus().name(),
            entity.getCompletedAt()
        );
    }
}
