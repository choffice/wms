package com.portfolio.warehouse.actionqueue.service;

import com.portfolio.warehouse.actionqueue.api.dto.*;
import com.portfolio.warehouse.handover.api.dto.HandoverRowResponse;
import com.portfolio.warehouse.handover.service.HandoverService;
import com.portfolio.warehouse.integrity.api.dto.IntegrityIssueResponse;
import com.portfolio.warehouse.integrity.service.IntegrityService;
import com.portfolio.warehouse.issue.domain.*;
import com.portfolio.warehouse.issue.repository.SpecialIssueRepository;
import com.portfolio.warehouse.operations.api.dto.MateOperationRow;
import com.portfolio.warehouse.operations.service.OperationsBoardService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActionQueueService {

    private final OperationsBoardService operationsBoardService;
    private final HandoverService handoverService;
    private final IntegrityService integrityService;
    private final SpecialIssueRepository issueRepository;

    public ActionQueueService(
        OperationsBoardService operationsBoardService,
        HandoverService handoverService,
        IntegrityService integrityService,
        SpecialIssueRepository issueRepository
    ) {
        this.operationsBoardService = operationsBoardService;
        this.handoverService = handoverService;
        this.integrityService = integrityService;
        this.issueRepository = issueRepository;
    }

    @Transactional(readOnly = true)
    public ActionQueueResponse queue() {
        List<ActionQueueItemResponse> items =
            new ArrayList<>();

        integrityItems(items);
        liveOperationItems(items);
        handoverItems(items);
        issueItems(items);

        items.sort(
            Comparator
                .<ActionQueueItemResponse>comparingInt(
                    item -> levelRank(item.level())
                )
                .thenComparing(
                    ActionQueueItemResponse::category
                )
                .thenComparing(
                    ActionQueueItemResponse::key
                )
        );

        List<ActionQueueItemResponse> limited =
            items.size() <= 100
                ? List.copyOf(items)
                : List.copyOf(
                    items.subList(0, 100)
                );

        return new ActionQueueResponse(
            LocalDateTime.now(),
            new ActionQueueSummaryResponse(
                items.size(),
                count(items, "BLOCKER"),
                count(items, "ATTENTION"),
                count(items, "HANDOVER"),
                count(items, "ISSUE")
            ),
            limited
        );
    }

    private void integrityItems(
        List<ActionQueueItemResponse> items
    ) {
        for (
            IntegrityIssueResponse issue :
            integrityService.scan().issues()
        ) {
            if (
                !"CRITICAL".equals(issue.severity())
                    && !"WARNING".equals(
                        issue.severity()
                    )
            ) {
                continue;
            }

            String level =
                "CRITICAL".equals(issue.severity())
                    ? "BLOCKER"
                    : "ATTENTION";

            items.add(
                new ActionQueueItemResponse(
                    "INTEGRITY:"
                        + issue.issueKey(),
                    level,
                    "DATA_INTEGRITY",
                    "BLOCKER".equals(level)
                        ? "정합성 치명 오류"
                        : "정합성 확인",
                    issue.subject(),
                    issue.detail(),
                    "정합성 열기",
                    "/integrity?keyword="
                        + encode(issue.subject()),
                    issue.entityType(),
                    issue.entityId()
                )
            );
        }
    }

    private void liveOperationItems(
        List<ActionQueueItemResponse> items
    ) {
        Set<String> directCodes =
            Set.of(
                "HEARTBEAT_STALE",
                "SESSION_UNCERTAIN",
                "NETWORK_RECOVERY_REQUIRED"
            );

        for (
            MateOperationRow row :
            operationsBoardService.board().mates()
        ) {
            for (String code : row.attentionCodes()) {
                if (!directCodes.contains(code)) {
                    continue;
                }

                items.add(
                    new ActionQueueItemResponse(
                        "OPS:"
                            + row.mateId()
                            + ":"
                            + code,
                        "ATTENTION",
                        "LIVE_OPERATION",
                        operationTitle(code),
                        row.nickname()
                            + " ("
                            + row.employeeNo()
                            + ")",
                        operationDetail(
                            code,
                            row
                        ),
                        "운영관제 열기",
                        "/operations?mateId="
                            + row.mateId(),
                        "MATE",
                        row.mateId()
                    )
                );
            }
        }
    }

    private void handoverItems(
        List<ActionQueueItemResponse> items
    ) {
        for (
            HandoverRowResponse row :
            handoverService.board().rows()
        ) {
            if (!row.handoverCandidate()) {
                continue;
            }

            items.add(
                new ActionQueueItemResponse(
                    "HANDOVER:"
                        + row.assignmentId(),
                    "HANDOVER",
                    "HANDOVER",
                    row.stateLabel(),
                    "Assignment #"
                        + row.assignmentId()
                        + " · "
                        + row.workType(),
                    row.currentMateNickname()
                        + " / "
                        + row.area()
                        + " / 이어갈 위치 "
                        + (
                            row.currentLastCompletedLocation()
                                == null
                                    ? row.startLocation()
                                    : row.currentLastCompletedLocation()
                        ),
                    "인수인계 열기",
                    "/handover?assignmentId="
                        + row.assignmentId(),
                    "WORK_ASSIGNMENT",
                    row.assignmentId()
                )
            );
        }
    }

    private void issueItems(
        List<ActionQueueItemResponse> items
    ) {
        for (
            SpecialIssue issue :
            issueRepository
                .findAllByDeletedAtIsNullOrderByCreatedAtDesc()
        ) {
            if (
                issue.getStatus()
                    == IssueStatus.RESOLVED
            ) {
                continue;
            }

            boolean unconfirmed =
                issue.getStatus()
                    == IssueStatus.UNCONFIRMED;

            boolean unassigned =
                issue.getResponsibleMate() == null;

            if (!unconfirmed && !unassigned) {
                continue;
            }

            String title;

            if (unconfirmed && unassigned) {
                title = "미확인 · 미담당 특이사항";
            } else if (unconfirmed) {
                title = "미확인 특이사항";
            } else {
                title = "미담당 특이사항";
            }

            items.add(
                new ActionQueueItemResponse(
                    "ISSUE:" + issue.getId(),
                    "ISSUE",
                    "SPECIAL_ISSUE",
                    title,
                    "#"
                        + issue.getId()
                        + " · "
                        + issue.getIssueType().getName(),
                    issue.getComment(),
                    "특이사항 열기",
                    "/issues?issueId="
                        + issue.getId(),
                    "SPECIAL_ISSUE",
                    issue.getId()
                )
            );
        }
    }

    private int count(
        List<ActionQueueItemResponse> items,
        String level
    ) {
        return (int) items.stream()
            .filter(item ->
                level.equals(item.level())
            )
            .count();
    }

    private int levelRank(String level) {
        return switch (level) {
            case "BLOCKER" -> 0;
            case "ATTENTION" -> 1;
            case "HANDOVER" -> 2;
            case "ISSUE" -> 3;
            default -> 9;
        };
    }

    private String operationTitle(String code) {
        return switch (code) {
            case "HEARTBEAT_STALE" ->
                "통신 지연 확인";
            case "SESSION_UNCERTAIN" ->
                "작업시간 신뢰도 확인";
            case "NETWORK_RECOVERY_REQUIRED" ->
                "통신 복귀 확인";
            default -> "운영 확인";
        };
    }

    private String operationDetail(
        String code,
        MateOperationRow row
    ) {
        return switch (code) {
            case "HEARTBEAT_STALE" ->
                "Open Session의 Heartbeat가 3분 이상 지연되었습니다.";
            case "SESSION_UNCERTAIN" ->
                "현재 WorkSession 품질이 UNCERTAIN입니다.";
            case "NETWORK_RECOVERY_REQUIRED" ->
                "MATE 거소가 '통신 확인 필요' 상태입니다.";
            default ->
                row.whereabouts() == null
                    ? "현재 상태를 확인해주세요."
                    : row.whereabouts();
        };
    }

    private String encode(String value) {
        return URLEncoder.encode(
            value,
            StandardCharsets.UTF_8
        );
    }
}
