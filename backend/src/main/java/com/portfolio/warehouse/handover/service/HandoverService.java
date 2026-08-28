package com.portfolio.warehouse.handover.service;

import com.portfolio.warehouse.handover.api.dto.*;
import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.mate.domain.MateStatus;
import com.portfolio.warehouse.pda.domain.PdaUsageHistory;
import com.portfolio.warehouse.pda.repository.PdaUsageHistoryRepository;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HandoverService {

    private final WorkAssignmentRepository assignmentRepository;
    private final WorkSessionRepository sessionRepository;
    private final PdaUsageHistoryRepository usageRepository;

    public HandoverService(
        WorkAssignmentRepository assignmentRepository,
        WorkSessionRepository sessionRepository,
        PdaUsageHistoryRepository usageRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.sessionRepository = sessionRepository;
        this.usageRepository = usageRepository;
    }

    @Transactional(readOnly = true)
    public HandoverBoardResponse board() {
        List<WorkSession> openSessions =
            sessionRepository.findAllByEndedAtIsNull();

        Set<Long> openAssignmentIds =
            openSessions.stream()
                .map(session ->
                    session.getWorkAssignment().getId()
                )
                .collect(java.util.stream.Collectors.toSet());

        Set<Long> busyMateIds =
            openSessions.stream()
                .map(session ->
                    session.getMate().getId()
                )
                .collect(java.util.stream.Collectors.toSet());

        Map<Long, PdaUsageHistory> activeUsageByMate =
            new HashMap<>();

        usageRepository.findAll().stream()
            .filter(PdaUsageHistory::isActiveUsage)
            .forEach(usage ->
                activeUsageByMate.putIfAbsent(
                    usage.getMate().getId(),
                    usage
                )
            );

        List<HandoverRowResponse> rows =
            assignmentRepository
                .findAllByOrderByAssignedAtDesc()
                .stream()
                .filter(this::isPending)
                .filter(assignment ->
                    !openAssignmentIds.contains(
                        assignment.getId()
                    )
                )
                .map(assignment ->
                    row(
                        assignment,
                        busyMateIds,
                        activeUsageByMate
                    )
                )
                .sorted(
                    Comparator
                        .<HandoverRowResponse>comparingInt(
                            row ->
                                stateRank(
                                    row.handoverState()
                                )
                        )
                        .thenComparing(
                            HandoverRowResponse::assignmentId,
                            Comparator.reverseOrder()
                        )
                )
                .toList();

        int handoverCandidates =
            (int) rows.stream()
                .filter(
                    HandoverRowResponse::handoverCandidate
                )
                .count();

        int assignedNotStarted =
            count(rows, "ASSIGNED_NOT_STARTED");

        int paused =
            count(rows, "PAUSED")
                + count(rows, "READY_TO_RESUME");

        int network =
            count(rows, "NETWORK_RECOVERY");

        int offDuty =
            count(rows, "OFF_DUTY_HANDOVER")
                + count(rows, "SHIFT_CARRYOVER");

        int busyElsewhere =
            (int) rows.stream()
                .filter(
                    HandoverRowResponse::mateBusyElsewhere
                )
                .count();

        return new HandoverBoardResponse(
            LocalDateTime.now(),
            new HandoverSummaryResponse(
                rows.size(),
                handoverCandidates,
                assignedNotStarted,
                paused,
                network,
                offDuty,
                busyElsewhere
            ),
            rows
        );
    }

    private HandoverRowResponse row(
        WorkAssignment assignment,
        Set<Long> busyMateIds,
        Map<Long, PdaUsageHistory> activeUsageByMate
    ) {
        Mate mate =
            assignment.getCurrentMate();

        List<WorkSession> sessions =
            sessionRepository
                .findAllByWorkAssignmentIdOrderByStartedAtAsc(
                    assignment.getId()
                );

        WorkSession last =
            sessions.isEmpty()
                ? null
                : sessions.get(sessions.size() - 1);

        State state =
            state(
                assignment,
                mate,
                last
            );

        PdaUsageHistory usage =
            activeUsageByMate.get(mate.getId());

        boolean busyElsewhere =
            busyMateIds.contains(mate.getId());

        return new HandoverRowResponse(
            assignment.getId(),
            assignment.getStatus().name(),
            state.code(),
            state.label(),
            assignment.getWorkType().getId(),
            assignment.getWorkType().getName(),
            assignment.getAreaLocation().getId(),
            assignment.getAreaLocation().getFullCode(),
            assignment.getStartLocation().getId(),
            assignment.getStartLocation().getFullCode(),
            assignment.getCurrentLastCompletedLocation()
                == null
                    ? null
                    : assignment
                        .getCurrentLastCompletedLocation()
                        .getId(),
            assignment.getCurrentLastCompletedLocation()
                == null
                    ? null
                    : assignment
                        .getCurrentLastCompletedLocation()
                        .getFullCode(),
            mate.getId(),
            mate.getEmployeeNo(),
            mate.getNickname(),
            mate.getCurrentStatus().name(),
            mate.getCurrentWhereabouts(),
            usage == null
                ? null
                : usage.getPdaDevice().getDeviceNumber(),
            last == null ? null : last.getId(),
            last == null ? null : last.getStartedAt(),
            last == null ? null : last.getEndedAt(),
            last == null || last.getEndReason() == null
                ? null
                : last.getEndReason().name(),
            last == null
                ? null
                : last.getQualityStatus().name(),
            busyElsewhere,
            state.handoverCandidate()
        );
    }

    private State state(
        WorkAssignment assignment,
        Mate mate,
        WorkSession last
    ) {
        if (
            (
                last != null
                    && last.getEndReason()
                        == WorkSessionEndReason.NETWORK_TIMEOUT
            )
                || "통신 확인 필요".equals(
                    mate.getCurrentWhereabouts()
                )
        ) {
            return new State(
                "NETWORK_RECOVERY",
                "통신 복귀 확인",
                true
            );
        }

        if (
            mate.getCurrentStatus()
                == MateStatus.OFF_DUTY
        ) {
            return new State(
                "OFF_DUTY_HANDOVER",
                "퇴근 인수인계",
                true
            );
        }

        if (
            last != null
                && (
                    last.getEndReason()
                        == WorkSessionEndReason.SCHEDULE_END
                    || last.getEndReason()
                        == WorkSessionEndReason.MANUAL_SHIFT_END
                    || last.getEndReason()
                        == WorkSessionEndReason.LOGOUT
                )
        ) {
            return new State(
                "SHIFT_CARRYOVER",
                "근무종료 이월",
                true
            );
        }

        if (
            assignment.getStatus()
                == WorkAssignmentStatus.ASSIGNED
                && last == null
        ) {
            return new State(
                "ASSIGNED_NOT_STARTED",
                "미시작 배정",
                false
            );
        }

        if (
            last != null
                && (
                    last.getEndReason()
                        == WorkSessionEndReason.PAUSED
                    || last.getEndReason()
                        == WorkSessionEndReason.TASK_SWITCH
                )
        ) {
            return new State(
                "PAUSED",
                "일시정지",
                false
            );
        }

        return new State(
            "READY_TO_RESUME",
            "재개 대기",
            false
        );
    }

    private boolean isPending(
        WorkAssignment assignment
    ) {
        return assignment.getStatus()
                == WorkAssignmentStatus.ASSIGNED
            || assignment.getStatus()
                == WorkAssignmentStatus.IN_PROGRESS;
    }

    private int count(
        List<HandoverRowResponse> rows,
        String state
    ) {
        return (int) rows.stream()
            .filter(row ->
                state.equals(row.handoverState())
            )
            .count();
    }

    private int stateRank(String state) {
        return switch (state) {
            case "NETWORK_RECOVERY" -> 0;
            case "OFF_DUTY_HANDOVER" -> 1;
            case "SHIFT_CARRYOVER" -> 2;
            case "ASSIGNED_NOT_STARTED" -> 3;
            case "PAUSED" -> 4;
            default -> 5;
        };
    }

    private record State(
        String code,
        String label,
        boolean handoverCandidate
    ) {}
}
