package com.portfolio.warehouse.handover.api.dto;

public record HandoverAssignmentBriefResponse(
    Long assignmentId,
    String stateLabel,
    String workType,
    String area,
    String currentMate,
    String lastLocation,
    String lastSessionEndReason
) {}
