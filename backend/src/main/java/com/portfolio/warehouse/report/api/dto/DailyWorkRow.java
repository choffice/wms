package com.portfolio.warehouse.report.api.dto;

public record DailyWorkRow(
    Long assignmentId,
    String mateNickname,
    Integer pdaNumber,
    String workType,
    String area,
    String startLocation,
    String lastCompletedLocation,
    long actualWorkSeconds,
    String qualityStatus
) {}
