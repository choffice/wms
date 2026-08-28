package com.portfolio.warehouse.dashboard.api.dto;

import com.portfolio.warehouse.issue.api.dto.SpecialIssueResponse;
import com.portfolio.warehouse.log.api.dto.ActivityLogResponse;
import com.portfolio.warehouse.notice.api.dto.NoticeResponse;
import java.util.List;

public record AdminDashboardResponse(
    List<NoticeResponse> notices,
    List<SpecialIssueResponse> issues,
    List<MateDashboardRow> mates,
    List<AreaWorkStatusRow> areaWorkStatuses,
    List<ActivityLogResponse> latestLogs
) {}
