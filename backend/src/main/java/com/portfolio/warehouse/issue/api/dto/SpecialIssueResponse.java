package com.portfolio.warehouse.issue.api.dto;

import com.portfolio.warehouse.issue.domain.SpecialIssue;
import java.time.LocalDateTime;

public record SpecialIssueResponse(
    Long id,
    Long issueTypeId,
    String issueType,
    Long authorMateId,
    String authorNickname,
    Long responsibleMateId,
    String responsibleNickname,
    Long workAssignmentId,
    String location,
    String productCode,
    Integer quantity,
    Integer actualStock,
    Integer mmsStock,
    Integer expiryStock,
    boolean noStock,
    String comment,
    String status,
    long viewCount,
    boolean isNew,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static SpecialIssueResponse from(SpecialIssue entity) {
        return new SpecialIssueResponse(
            entity.getId(),
            entity.getIssueType().getId(),
            entity.getIssueType().getName(),
            entity.getAuthorMate().getId(),
            entity.getAuthorMate().getNickname(),
            entity.getResponsibleMate() == null ? null : entity.getResponsibleMate().getId(),
            entity.getResponsibleMate() == null ? null : entity.getResponsibleMate().getNickname(),
            entity.getWorkAssignment() == null ? null : entity.getWorkAssignment().getId(),
            entity.getLocation() == null ? null : entity.getLocation().getFullCode(),
            entity.getProductCode(),
            entity.getQuantity(),
            entity.getActualStock(),
            entity.getMmsStock(),
            entity.getExpiryStock(),
            entity.isNoStock(),
            entity.getComment(),
            entity.getStatus().name(),
            entity.getViewCount(),
            entity.getViewCount() == 0,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
