package com.portfolio.warehouse.notice.api.dto;

import com.portfolio.warehouse.notice.domain.Notice;
import java.time.LocalDateTime;

public record NoticeResponse(
    Long id,
    String content,
    boolean visible,
    boolean important,
    int displayOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static NoticeResponse from(Notice entity) {
        return new NoticeResponse(
            entity.getId(),
            entity.getContent(),
            entity.isVisible(),
            entity.isImportant(),
            entity.getDisplayOrder(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
