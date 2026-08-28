package com.portfolio.warehouse.issue.api.dto;

import com.portfolio.warehouse.issue.domain.SpecialIssueHistory;
import java.time.LocalDateTime;

public record SpecialIssueHistoryResponse(
    Long id,
    String actionType,
    String fromResponsibleNickname,
    String toResponsibleNickname,
    String actor,
    String reason,
    LocalDateTime changedAt
) {
    public static SpecialIssueHistoryResponse from(
        SpecialIssueHistory entity
    ) {
        return new SpecialIssueHistoryResponse(
            entity.getId(),
            entity.getActionType().name(),
            entity.getFromResponsibleMate() == null
                ? null
                : entity.getFromResponsibleMate().getNickname(),
            entity.getToResponsibleMate() == null
                ? null
                : entity.getToResponsibleMate().getNickname(),
            entity.getActorAccount().getLoginId(),
            entity.getReason(),
            entity.getChangedAt()
        );
    }
}
