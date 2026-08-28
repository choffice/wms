package com.portfolio.warehouse.work.api.dto;

import com.portfolio.warehouse.work.domain.WorkSession;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkSessionResponse(
    Long id,
    Long assignmentId,
    String mateNickname,
    Integer pdaNumber,
    LocalDate shiftDate,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    String endReason,
    LocalDateTime lastHeartbeatAt,
    String qualityStatus,
    Long durationSeconds
) {
    public static WorkSessionResponse from(WorkSession entity) {
        return new WorkSessionResponse(
            entity.getId(),
            entity.getWorkAssignment().getId(),
            entity.getMate().getNickname(),
            entity.getPdaUsageHistory().getPdaDevice().getDeviceNumber(),
            entity.getShiftDate(),
            entity.getStartedAt(),
            entity.getEndedAt(),
            entity.getEndReason() == null ? null : entity.getEndReason().name(),
            entity.getLastHeartbeatAt(),
            entity.getQualityStatus().name(),
            entity.getEndedAt() == null ? null : entity.getDuration().getSeconds()
        );
    }
}
