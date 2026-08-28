package com.portfolio.warehouse.work.api.dto;

import com.portfolio.warehouse.work.domain.WorkAssignmentHistory;
import java.time.LocalDateTime;

public record WorkAssignmentHistoryResponse(
    Long id,
    String actionType,
    String fromMateNickname,
    String toMateNickname,
    String actor,
    String reason,
    LocalDateTime changedAt
) {
    public static WorkAssignmentHistoryResponse from(
        WorkAssignmentHistory entity
    ) {
        return new WorkAssignmentHistoryResponse(
            entity.getId(),
            entity.getActionType().name(),
            entity.getFromMate() == null
                ? null
                : entity.getFromMate().getNickname(),
            entity.getToMate() == null
                ? null
                : entity.getToMate().getNickname(),
            entity.getActorAccount().getLoginId(),
            entity.getReason(),
            entity.getChangedAt()
        );
    }
}
