package com.portfolio.warehouse.shiftclose.service;

import com.portfolio.warehouse.handover.api.dto.HandoverBoardResponse;
import com.portfolio.warehouse.handover.service.HandoverService;
import com.portfolio.warehouse.integrity.api.dto.IntegrityScanResponse;
import com.portfolio.warehouse.integrity.service.IntegrityService;
import com.portfolio.warehouse.issue.domain.IssueStatus;
import com.portfolio.warehouse.issue.repository.SpecialIssueRepository;
import com.portfolio.warehouse.operations.api.dto.OperationsBoardResponse;
import com.portfolio.warehouse.operations.service.OperationsBoardService;
import com.portfolio.warehouse.report.service.ReportService;
import com.portfolio.warehouse.shiftclose.api.dto.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftClosePreviewService {

    private final OperationsBoardService operationsBoardService;
    private final HandoverService handoverService;
    private final IntegrityService integrityService;
    private final SpecialIssueRepository issueRepository;
    private final ReportService reportService;

    public ShiftClosePreviewService(
        OperationsBoardService operationsBoardService,
        HandoverService handoverService,
        IntegrityService integrityService,
        SpecialIssueRepository issueRepository,
        ReportService reportService
    ) {
        this.operationsBoardService = operationsBoardService;
        this.handoverService = handoverService;
        this.integrityService = integrityService;
        this.issueRepository = issueRepository;
        this.reportService = reportService;
    }

    @Transactional(readOnly = true)
    public ShiftClosePreviewResponse preview() {
        OperationsBoardResponse operations =
            operationsBoardService.board();

        HandoverBoardResponse handover =
            handoverService.board();

        IntegrityScanResponse integrity =
            integrityService.scan();

        int unresolvedIssues =
            (int) issueRepository
                .findAllByDeletedAtIsNullOrderByCreatedAtDesc()
                .stream()
                .filter(issue ->
                    issue.getStatus()
                        != IssueStatus.RESOLVED
                )
                .count();

        List<ShiftCloseCheckResponse> checks =
            new ArrayList<>();

        checks.add(
            check(
                "OPEN_WORK_SESSION",
                operations.summary().activeSessionCount() > 0
                    ? "BLOCKER"
                    : "OK",
                "진행 중 WorkSession",
                operations.summary().activeSessionCount(),
                operations.summary().activeSessionCount() > 0
                    ? "실제 작업시간이 계속 측정 중입니다. 교대 마감 전에 각 MATE의 일시정지/작업종료 상태를 확인하세요."
                    : "현재 Open WorkSession이 없습니다.",
                "운영관제",
                "/operations"
            )
        );

        checks.add(
            check(
                "INTEGRITY_CRITICAL",
                integrity.summary().critical() > 0
                    ? "BLOCKER"
                    : "OK",
                "치명 정합성 오류",
                integrity.summary().critical(),
                integrity.summary().critical() > 0
                    ? "중복 Session 또는 담당/Usage 불일치처럼 수동 확인이 필요한 치명 오류가 있습니다."
                    : "현재 검사범위에서 치명 정합성 오류가 없습니다.",
                "정합성",
                "/integrity"
            )
        );

        checks.add(
            check(
                "UNCERTAIN_SESSION",
                operations.summary().uncertainSessionCount() > 0
                    ? "WARNING"
                    : "OK",
                "UNCERTAIN Session",
                operations.summary().uncertainSessionCount(),
                operations.summary().uncertainSessionCount() > 0
                    ? "통신/Heartbeat 문제로 신뢰도가 낮은 작업시간이 있습니다. 보고서 확정 전 확인을 권장합니다."
                    : "UNCERTAIN 상태의 현재 Session이 없습니다.",
                "운영관제",
                "/operations"
            )
        );

        checks.add(
            check(
                "PDA_IN_USE",
                operations.summary().pdaInUseCount() > 0
                    ? "WARNING"
                    : "OK",
                "사용 중 PDA",
                operations.summary().pdaInUseCount(),
                operations.summary().pdaInUseCount() > 0
                    ? "아직 IN_USE인 PDA가 있습니다. 근무중 MATE의 정상 사용인지 반납 누락인지 확인하세요."
                    : "현재 IN_USE PDA가 없습니다.",
                "운영관제",
                "/operations"
            )
        );

        checks.add(
            check(
                "HANDOVER_CANDIDATE",
                handover.summary().handoverCandidateCount() > 0
                    ? "WARNING"
                    : "OK",
                "인수인계 검토 업무",
                handover.summary().handoverCandidateCount(),
                handover.summary().handoverCandidateCount() > 0
                    ? "퇴근·근무종료·통신복귀 등으로 담당자 인수인계를 검토할 업무가 있습니다."
                    : "현재 즉시 인수인계를 검토할 업무가 없습니다.",
                "인수인계",
                "/handover"
            )
        );

        checks.add(
            check(
                "PENDING_ASSIGNMENT",
                handover.summary().pendingCount() > 0
                    ? "WARNING"
                    : "OK",
                "미처리 활성업무",
                handover.summary().pendingCount(),
                handover.summary().pendingCount() > 0
                    ? "완료되지 않은 활성 Assignment가 남아 있습니다. 그대로 재개할지 다음 교대로 넘길지 확인하세요."
                    : "현재 미처리 활성업무가 없습니다.",
                "인수인계",
                "/handover"
            )
        );

        checks.add(
            check(
                "UNCONFIRMED_ISSUE",
                operations.summary().unconfirmedIssueCount() > 0
                    ? "WARNING"
                    : "OK",
                "미확인 특이사항",
                operations.summary().unconfirmedIssueCount(),
                operations.summary().unconfirmedIssueCount() > 0
                    ? "아직 관리자 확인이 되지 않은 특이사항이 있습니다."
                    : "미확인 특이사항이 없습니다.",
                "특이사항",
                "/issues?status=UNCONFIRMED"
            )
        );

        checks.add(
            check(
                "UNASSIGNED_ISSUE",
                operations.summary().unassignedOpenIssueCount() > 0
                    ? "WARNING"
                    : "OK",
                "미담당 특이사항",
                operations.summary().unassignedOpenIssueCount(),
                operations.summary().unassignedOpenIssueCount() > 0
                    ? "해결되지 않은 특이사항 중 담당자가 지정되지 않은 건이 있습니다."
                    : "미담당 상태의 열린 특이사항이 없습니다.",
                "특이사항",
                "/issues?responsible=UNASSIGNED"
            )
        );

        checks.add(
            check(
                "UNRESOLVED_ISSUE",
                unresolvedIssues > 0
                    ? "WARNING"
                    : "OK",
                "미해결 특이사항",
                unresolvedIssues,
                unresolvedIssues > 0
                    ? "다음 교대에서도 이어서 확인해야 할 미해결 특이사항이 있습니다."
                    : "현재 미해결 특이사항이 없습니다.",
                "특이사항",
                "/issues"
            )
        );

        int blocker =
            (int) checks.stream()
                .filter(item ->
                    "BLOCKER".equals(item.level())
                )
                .count();

        int warning =
            (int) checks.stream()
                .filter(item ->
                    "WARNING".equals(item.level())
                )
                .count();

        int ok =
            (int) checks.stream()
                .filter(item ->
                    "OK".equals(item.level())
                )
                .count();

        return new ShiftClosePreviewResponse(
            LocalDateTime.now(),
            new ShiftCloseSummaryResponse(
                blocker,
                warning,
                ok,
                blocker == 0
            ),
            reportService.recentShiftDates(7),
            List.copyOf(checks)
        );
    }

    private ShiftCloseCheckResponse check(
        String code,
        String level,
        String label,
        int count,
        String description,
        String actionLabel,
        String actionPath
    ) {
        return new ShiftCloseCheckResponse(
            code,
            level,
            label,
            count,
            description,
            actionLabel,
            actionPath
        );
    }
}
