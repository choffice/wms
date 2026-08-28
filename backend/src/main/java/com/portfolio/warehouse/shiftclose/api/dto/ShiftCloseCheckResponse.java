package com.portfolio.warehouse.shiftclose.api.dto;

public record ShiftCloseCheckResponse(
    String code,
    String level,
    String label,
    int count,
    String description,
    String actionLabel,
    String actionPath
) {}
