package com.portfolio.warehouse.work.api.dto;

public record WorkEstimateResponse(
    Long areaId,
    String areaCode,
    Long workTypeId,
    String workType,
    Long selectedStartLocationId,
    String selectedStartLocation,
    Integer selectedStartPercent,
    String currentLastCompletedLocation,
    int currentProgressPercent,
    Long estimatedFullAreaSeconds,
    Long estimatedRemainingFromCurrentSeconds,
    Long estimatedRemainingFromSelectedStartSeconds,
    int historicalSampleCount
) {}
