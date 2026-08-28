package com.portfolio.warehouse.handover.api.dto;

import com.portfolio.warehouse.log.api.dto.ActivityLogResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HandoverOverviewResponse(
    LocalDateTime generatedAt,
    HandoverOverviewCountsResponse counts,
    List<String> summaryLines,
    List<LocalDate> recentShiftDates,
    List<HandoverAssignmentBriefResponse> assignments,
    List<HandoverIssueBriefResponse> issues,
    List<HandoverNoteResponse> recentNotes,
    List<ActivityLogResponse> recentAdminActions
) {}
