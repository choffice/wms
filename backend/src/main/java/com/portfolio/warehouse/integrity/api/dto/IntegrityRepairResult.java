package com.portfolio.warehouse.integrity.api.dto;

public record IntegrityRepairResult(
    int repairedCount,
    String message
) {}
