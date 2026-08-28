package com.portfolio.warehouse.handover.api.dto;

import com.portfolio.warehouse.handover.domain.HandoverNote;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record HandoverNoteResponse(
    Long id,
    String actor,
    LocalDate shiftDate,
    String content,
    LocalDateTime createdAt
) {
    public static HandoverNoteResponse from(
        HandoverNote entity
    ) {
        return new HandoverNoteResponse(
            entity.getId(),
            entity.getCreatedBy().getLoginId(),
            entity.getShiftDate(),
            entity.getContent(),
            entity.getCreatedAt()
        );
    }
}
