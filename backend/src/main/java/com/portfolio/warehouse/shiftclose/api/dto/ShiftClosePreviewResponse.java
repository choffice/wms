package com.portfolio.warehouse.shiftclose.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ShiftClosePreviewResponse(
    LocalDateTime generatedAt,
    ShiftCloseSummaryResponse summary,
    List<LocalDate> recentShiftDates,
    List<ShiftCloseCheckResponse> checks
) {}
