package com.portfolio.warehouse.handover.api.dto;

import com.portfolio.warehouse.work.api.dto.WorkAssignmentResponse;
import java.util.List;

public record BulkHandoverResultResponse(
    int processedCount,
    List<WorkAssignmentResponse> assignments
) {}
