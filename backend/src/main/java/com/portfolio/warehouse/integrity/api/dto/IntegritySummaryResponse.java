package com.portfolio.warehouse.integrity.api.dto;

public record IntegritySummaryResponse(
    int total,
    int critical,
    int warning,
    int repairable
) {}
