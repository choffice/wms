package com.portfolio.warehouse.report.api.dto;

public record MateWorkStatRow(
    Long mateId,
    String employeeNo,
    String nickname,
    long sessionCount,
    long assignmentCount,
    long normalSeconds,
    long uncertainSeconds
) {}
