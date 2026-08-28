package com.portfolio.warehouse.handover.api.dto;

import java.time.LocalDateTime;

public record HandoverRowResponse(
    Long assignmentId,
    String assignmentStatus,
    String handoverState,
    String stateLabel,
    Long workTypeId,
    String workType,
    Long areaId,
    String area,
    Long startLocationId,
    String startLocation,
    Long currentLastCompletedLocationId,
    String currentLastCompletedLocation,
    Long currentMateId,
    String employeeNo,
    String currentMateNickname,
    String currentMateStatus,
    String currentMateWhereabouts,
    Integer currentPdaNumber,
    Long lastSessionId,
    LocalDateTime lastSessionStartedAt,
    LocalDateTime lastSessionEndedAt,
    String lastSessionEndReason,
    String lastSessionQuality,
    boolean mateBusyElsewhere,
    boolean handoverCandidate
) {}
