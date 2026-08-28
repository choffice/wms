package com.portfolio.warehouse.dashboard.api.dto;

public record MateDashboardRow(
    Long mateId,
    String nickname,
    String status,
    String whereabouts,
    Integer pdaNumber,
    Long assignmentId,
    String workType,
    String area,
    String lastCompletedLocation
) {}
