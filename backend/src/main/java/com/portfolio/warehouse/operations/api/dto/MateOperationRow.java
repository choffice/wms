package com.portfolio.warehouse.operations.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MateOperationRow(
    Long mateId,
    String employeeNo,
    String nickname,
    String status,
    String whereabouts,
    Long pdaUsageId,
    Long pdaDeviceId,
    Integer pdaNumber,
    String pdaStatus,
    Long assignmentId,
    String assignmentStatus,
    String workType,
    String area,
    String startLocation,
    String lastCompletedLocation,
    Long openSessionId,
    LocalDateTime sessionStartedAt,
    LocalDateTime lastHeartbeatAt,
    String sessionQuality,
    Long elapsedSeconds,
    LocalDate shiftDate,
    LocalDateTime effectiveScheduledEnd,
    boolean extensionActive,
    List<String> attentionCodes
) {}
