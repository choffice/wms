package com.portfolio.warehouse.readiness.api.dto;

public record SystemReadinessCheck(
    String code,
    String level,
    String label,
    String detail,
    String actionPath
) {}
