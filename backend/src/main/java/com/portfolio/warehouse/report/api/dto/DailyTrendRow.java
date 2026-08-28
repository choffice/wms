package com.portfolio.warehouse.report.api.dto;

import java.time.LocalDate;

public record DailyTrendRow(
    LocalDate date,
    long normalSeconds,
    long uncertainSeconds,
    long issueCount
) {}
