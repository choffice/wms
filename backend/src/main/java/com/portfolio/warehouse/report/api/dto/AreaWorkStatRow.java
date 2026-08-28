package com.portfolio.warehouse.report.api.dto;

public record AreaWorkStatRow(
    Long areaId,
    String area,
    Long workTypeId,
    String workType,
    long sessionCount,
    long assignmentCount,
    long normalSeconds,
    long uncertainSeconds
) {}
