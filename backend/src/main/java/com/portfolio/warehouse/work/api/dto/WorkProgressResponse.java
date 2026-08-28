package com.portfolio.warehouse.work.api.dto;

import com.portfolio.warehouse.work.domain.WorkProgress;
import java.time.LocalDateTime;

public record WorkProgressResponse(
    Long id,
    Long assignmentId,
    String mateNickname,
    String reportedBy,
    String previousLocation,
    String lastCompletedLocation,
    LocalDateTime reportedAt,
    boolean correction,
    String reason
) {
    public static WorkProgressResponse from(
        WorkProgress entity
    ) {
        return new WorkProgressResponse(
            entity.getId(),
            entity.getWorkAssignment().getId(),
            entity.getMate().getNickname(),
            entity.getReportedByAccount() == null
                ? null
                : entity.getReportedByAccount().getLoginId(),
            entity.getPreviousLocation() == null
                ? null
                : entity.getPreviousLocation().getFullCode(),
            entity.getLastCompletedLocation().getFullCode(),
            entity.getReportedAt(),
            entity.isCorrection(),
            entity.getReason()
        );
    }
}
