package com.portfolio.warehouse.pda.api.dto;

import com.portfolio.warehouse.pda.domain.PdaUsageHistory;
import java.time.LocalDateTime;

public record PdaUsageResponse(
    Long usageId,
    Long deviceId,
    Integer deviceNumber,
    Long mateId,
    String employeeNo,
    String nickname,
    LocalDateTime assignedAt,
    LocalDateTime releasedAt,
    String releaseReason
) {
    public static PdaUsageResponse from(PdaUsageHistory entity) {
        return new PdaUsageResponse(
            entity.getId(),
            entity.getPdaDevice().getId(),
            entity.getPdaDevice().getDeviceNumber(),
            entity.getMate().getId(),
            entity.getMate().getEmployeeNo(),
            entity.getMate().getNickname(),
            entity.getAssignedAt(),
            entity.getReleasedAt(),
            entity.getReleaseReason() == null ? null : entity.getReleaseReason().name()
        );
    }
}
