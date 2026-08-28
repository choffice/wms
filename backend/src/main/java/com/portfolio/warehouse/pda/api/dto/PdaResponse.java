package com.portfolio.warehouse.pda.api.dto;

import com.portfolio.warehouse.pda.domain.PdaDevice;
import com.portfolio.warehouse.pda.domain.PdaStatus;
import java.time.LocalDateTime;

public record PdaResponse(
    Long id,
    Integer deviceNumber,
    PdaStatus status,
    boolean active,
    LocalDateTime createdAt
) {
    public static PdaResponse from(PdaDevice entity) {
        return new PdaResponse(
            entity.getId(),
            entity.getDeviceNumber(),
            entity.getStatus(),
            entity.isActive(),
            entity.getCreatedAt()
        );
    }
}
