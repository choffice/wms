package com.portfolio.warehouse.dashboard.api.dto;

import java.time.LocalDateTime;

public record AreaWorkStatusRow(
    Long areaId,
    String areaCode,
    Long workTypeId,
    String workType,
    String lastCompletedLocation,
    LocalDateTime lastPerformedAt,
    String lastMateNickname,
    int progressPercent,
    Long estimatedRemainingSeconds,
    int estimateSampleCount
) {}
