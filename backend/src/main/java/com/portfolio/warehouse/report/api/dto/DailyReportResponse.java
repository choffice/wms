package com.portfolio.warehouse.report.api.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyReportResponse(
    LocalDate date,
    List<DailyWorkRow> works,
    List<DailyPdaRow> pdaUsages,
    List<DailyIssueRow> issues
) {}
