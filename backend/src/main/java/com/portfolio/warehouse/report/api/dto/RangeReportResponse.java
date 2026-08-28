package com.portfolio.warehouse.report.api.dto;

import java.time.LocalDate;
import java.util.List;

public record RangeReportResponse(
    LocalDate from,
    LocalDate to,
    RangeReportSummary summary,
    List<MateWorkStatRow> mates,
    List<AreaWorkStatRow> areaWorks,
    List<DailyTrendRow> dailyTrend
) {}
