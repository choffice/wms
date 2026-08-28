package com.portfolio.warehouse.handover.service;

import com.portfolio.warehouse.handover.api.dto.*;
import com.portfolio.warehouse.integrity.api.dto.IntegrityScanResponse;
import com.portfolio.warehouse.integrity.service.IntegrityService;
import com.portfolio.warehouse.issue.domain.IssueStatus;
import com.portfolio.warehouse.issue.repository.SpecialIssueRepository;
import com.portfolio.warehouse.log.service.ActivityLogService;
import com.portfolio.warehouse.operations.api.dto.OperationsBoardResponse;
import com.portfolio.warehouse.operations.service.OperationsBoardService;
import com.portfolio.warehouse.report.service.ReportService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HandoverOverviewService {

    private final HandoverService handoverService;
    private final HandoverNoteService noteService;
    private final IntegrityService integrityService;
    private final OperationsBoardService operationsBoardService;
    private final SpecialIssueRepository issueRepository;
    private final ActivityLogService activityLogService;
    private final ReportService reportService;

    public HandoverOverviewService(
        HandoverService handoverService,
        HandoverNoteService noteService,
        IntegrityService integrityService,
        OperationsBoardService operationsBoardService,
        SpecialIssueRepository issueRepository,
        ActivityLogService activityLogService,
        ReportService reportService
    ) {
        this.handoverService = handoverService;
        this.noteService = noteService;
        this.integrityService = integrityService;
        this.operationsBoardService = operationsBoardService;
        this.issueRepository = issueRepository;
        this.activityLogService = activityLogService;
        this.reportService = reportService;
    }

    @Transactional(readOnly = true)
    public HandoverOverviewResponse overview() {
        var handover = handoverService.board();
        IntegrityScanResponse integrity =
            integrityService.scan();
        OperationsBoardResponse operations =
            operationsBoardService.board();

        var unresolved =
            issueRepository
                .findAllByDeletedAtIsNullOrderByCreatedAtDesc()
                .stream()
                .filter(issue ->
                    issue.getStatus()
                        != IssueStatus.RESOLVED
                )
                .toList();

        int unconfirmed =
            (int) unresolved.stream()
                .filter(issue ->
                    issue.getStatus()
                        == IssueStatus.UNCONFIRMED
                )
                .count();

        int unassigned =
            (int) unresolved.stream()
                .filter(issue ->
                    issue.getResponsibleMate() == null
                )
                .count();

        var counts =
            new HandoverOverviewCountsResponse(
                handover.summary().pendingCount(),
                handover.summary()
                    .handoverCandidateCount(),
                unresolved.size(),
                unconfirmed,
                unassigned,
                integrity.summary().critical(),
                integrity.summary().warning(),
                operations.summary().activeSessionCount(),
                operations.summary().attentionMateCount()
            );

        List<String> summaryLines =
            new ArrayList<>();

        summaryLines.add(
            "미처리 활성업무 "
                + counts.pendingAssignments()
                + "건 / 인수인계 검토 "
                + counts.handoverCandidates()
                + "건"
        );

        summaryLines.add(
            "미해결 특이사항 "
                + counts.unresolvedIssues()
                + "건 (미확인 "
                + counts.unconfirmedIssues()
                + " / 미담당 "
                + counts.unassignedIssues()
                + ")"
        );

        summaryLines.add(
            "정합성 치명 "
                + counts.integrityCritical()
                + "건 / 경고 "
                + counts.integrityWarning()
                + "건"
        );

        summaryLines.add(
            "Open WorkSession "
                + counts.openSessions()
                + "건 / 운영 Attention MATE "
                + counts.operationAttentionMates()
                + "명"
        );

        List<HandoverAssignmentBriefResponse>
            assignmentBriefs =
                handover.rows().stream()
                    .limit(30)
                    .map(row ->
                        new HandoverAssignmentBriefResponse(
                            row.assignmentId(),
                            row.stateLabel(),
                            row.workType(),
                            row.area(),
                            row.currentMateNickname(),
                            row.currentLastCompletedLocation()
                                == null
                                    ? row.startLocation()
                                    : row.currentLastCompletedLocation(),
                            row.lastSessionEndReason()
                        )
                    )
                    .toList();

        List<HandoverIssueBriefResponse>
            issueBriefs =
                unresolved.stream()
                    .limit(30)
                    .map(issue ->
                        new HandoverIssueBriefResponse(
                            issue.getId(),
                            issue.getStatus().name(),
                            issue.getIssueType().getName(),
                            issue.getResponsibleMate() == null
                                ? null
                                : issue
                                    .getResponsibleMate()
                                    .getNickname(),
                            issue.getLocation() == null
                                ? null
                                : issue
                                    .getLocation()
                                    .getFullCode(),
                            issue.getComment()
                        )
                    )
                    .toList();

        return new HandoverOverviewResponse(
            LocalDateTime.now(),
            counts,
            List.copyOf(summaryLines),
            reportService.recentShiftDates(7),
            assignmentBriefs,
            issueBriefs,
            noteService.recent(),
            activityLogService.recentAdminActions(20)
        );
    }
}
