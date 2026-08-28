package com.portfolio.warehouse.operations.service;

import com.portfolio.warehouse.issue.domain.IssueStatus;
import com.portfolio.warehouse.issue.repository.SpecialIssueRepository;
import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.mate.domain.MateStatus;
import com.portfolio.warehouse.mate.repository.MateRepository;
import com.portfolio.warehouse.mate.service.WorkScheduleResolver;
import com.portfolio.warehouse.operations.api.dto.*;
import com.portfolio.warehouse.pda.domain.*;
import com.portfolio.warehouse.pda.repository.*;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationsBoardService {

    private final MateRepository mateRepository;
    private final PdaUsageHistoryRepository pdaUsageRepository;
    private final PdaDeviceRepository pdaDeviceRepository;
    private final WorkAssignmentRepository assignmentRepository;
    private final WorkSessionRepository sessionRepository;
    private final SpecialIssueRepository issueRepository;
    private final WorkScheduleResolver scheduleResolver;

    public OperationsBoardService(
        MateRepository mateRepository,
        PdaUsageHistoryRepository pdaUsageRepository,
        PdaDeviceRepository pdaDeviceRepository,
        WorkAssignmentRepository assignmentRepository,
        WorkSessionRepository sessionRepository,
        SpecialIssueRepository issueRepository,
        WorkScheduleResolver scheduleResolver
    ) {
        this.mateRepository = mateRepository;
        this.pdaUsageRepository = pdaUsageRepository;
        this.pdaDeviceRepository = pdaDeviceRepository;
        this.assignmentRepository = assignmentRepository;
        this.sessionRepository = sessionRepository;
        this.issueRepository = issueRepository;
        this.scheduleResolver = scheduleResolver;
    }

    @Transactional(readOnly = true)
    public OperationsBoardResponse board() {
        LocalDateTime now = LocalDateTime.now();
        List<Mate> mates = mateRepository.findAllByActiveTrueOrderByNicknameAsc();
        List<MateOperationRow> rows = mates.stream()
            .map(mate -> row(mate, now))
            .toList();

        int activeSessions = (int) rows.stream()
            .filter(row -> row.openSessionId() != null)
            .count();

        int uncertainSessions = (int) rows.stream()
            .filter(row -> "UNCERTAIN".equals(row.sessionQuality()))
            .count();

        int attentionMates = (int) rows.stream()
            .filter(row -> !row.attentionCodes().isEmpty())
            .count();

        int pdaInUse = (int) pdaDeviceRepository.findAll().stream()
            .filter(device -> device.getStatus() == PdaStatus.IN_USE)
            .count();

        int pdaAttention = (int) pdaDeviceRepository.findAll().stream()
            .filter(device ->
                device.getStatus() == PdaStatus.LOST
                    || device.getStatus() == PdaStatus.INSPECTION
            )
            .count();

        int unconfirmedIssues =
            issueRepository
                .findAllByDeletedAtIsNullAndStatusOrderByCreatedAtDesc(
                    IssueStatus.UNCONFIRMED
                )
                .size();

        int unassignedOpenIssues =
            (int) issueRepository
                .findAllByDeletedAtIsNullOrderByCreatedAtDesc()
                .stream()
                .filter(issue ->
                    issue.getStatus() != IssueStatus.RESOLVED
                        && issue.getResponsibleMate() == null
                )
                .count();

        OperationsSummaryResponse summary =
            new OperationsSummaryResponse(
                rows.size(),
                countStatus(rows, MateStatus.AVAILABLE),
                countStatus(rows, MateStatus.WORKING),
                countStatus(rows, MateStatus.BREAK),
                countStatus(rows, MateStatus.AWAY),
                activeSessions,
                uncertainSessions,
                pdaInUse,
                pdaAttention,
                unconfirmedIssues,
                unassignedOpenIssues,
                attentionMates
            );

        return new OperationsBoardResponse(
            now,
            summary,
            rows
        );
    }

    private MateOperationRow row(
        Mate mate,
        LocalDateTime now
    ) {
        PdaUsageHistory usage =
            pdaUsageRepository
                .findFirstByMateIdAndReleasedAtIsNull(mate.getId())
                .orElse(null);

        WorkSession openSession =
            sessionRepository
                .findFirstByMateIdAndEndedAtIsNull(mate.getId())
                .orElse(null);

        WorkAssignment assignment = openSession != null
            ? openSession.getWorkAssignment()
            : assignmentRepository
                .findAllByCurrentMateIdAndStatusInOrderByAssignedAtDesc(
                    mate.getId(),
                    List.of(
                        WorkAssignmentStatus.IN_PROGRESS,
                        WorkAssignmentStatus.ASSIGNED
                    )
                )
                .stream()
                .findFirst()
                .orElse(null);

        LocalDate shiftDate =
            openSession != null
                && openSession.getShiftDate() != null
                    ? openSession.getShiftDate()
                    : openSession != null
                        ? scheduleResolver.resolveShiftDate(
                            mate.getId(),
                            openSession.getStartedAt()
                        )
                        : scheduleResolver.resolveShiftDate(
                            mate.getId(),
                            now
                        );

        boolean extension =
            scheduleResolver.extensionActive(
                mate.getId(),
                shiftDate
            );

        LocalDateTime effectiveEnd =
            scheduleResolver.effectiveEnd(
                mate.getId(),
                shiftDate
            ).orElse(null);

        List<String> attention = new ArrayList<>();

        if (openSession != null) {
            if (
                openSession.getLastHeartbeatAt() != null
                    && openSession.getLastHeartbeatAt()
                        .isBefore(now.minusMinutes(3))
            ) {
                attention.add("HEARTBEAT_STALE");
            }

            if (
                openSession.getQualityStatus()
                    == WorkSessionQualityStatus.UNCERTAIN
            ) {
                attention.add("SESSION_UNCERTAIN");
            }

            if (mate.getCurrentStatus() != MateStatus.WORKING) {
                attention.add("SESSION_STATUS_MISMATCH");
            }
        } else if (mate.getCurrentStatus() == MateStatus.WORKING) {
            attention.add("WORKING_WITHOUT_SESSION");
        }

        if (
            mate.getCurrentStatus() == MateStatus.OFF_DUTY
                && usage != null
        ) {
            attention.add("OFF_DUTY_WITH_PDA");
        }

        if (
            usage != null
                && usage.getPdaDevice().getStatus() == PdaStatus.LOST
        ) {
            attention.add("ACTIVE_PDA_MARKED_LOST");
        }

        if (
            mate.getCurrentStatus() == MateStatus.AWAY
                && "통신 확인 필요".equals(
                    mate.getCurrentWhereabouts()
                )
        ) {
            attention.add("NETWORK_RECOVERY_REQUIRED");
        }

        Long elapsedSeconds = openSession == null
            ? null
            : Math.max(
                0L,
                Duration.between(
                    openSession.getStartedAt(),
                    now
                ).getSeconds()
            );

        return new MateOperationRow(
            mate.getId(),
            mate.getEmployeeNo(),
            mate.getNickname(),
            mate.getCurrentStatus().name(),
            mate.getCurrentWhereabouts(),
            usage == null ? null : usage.getId(),
            usage == null ? null : usage.getPdaDevice().getId(),
            usage == null ? null : usage.getPdaDevice().getDeviceNumber(),
            usage == null ? null : usage.getPdaDevice().getStatus().name(),
            assignment == null ? null : assignment.getId(),
            assignment == null ? null : assignment.getStatus().name(),
            assignment == null ? null : assignment.getWorkType().getName(),
            assignment == null ? null : assignment.getAreaLocation().getFullCode(),
            assignment == null ? null : assignment.getStartLocation().getFullCode(),
            assignment == null
                || assignment.getCurrentLastCompletedLocation() == null
                ? null
                : assignment.getCurrentLastCompletedLocation().getFullCode(),
            openSession == null ? null : openSession.getId(),
            openSession == null ? null : openSession.getStartedAt(),
            openSession == null ? null : openSession.getLastHeartbeatAt(),
            openSession == null
                ? null
                : openSession.getQualityStatus().name(),
            elapsedSeconds,
            shiftDate,
            effectiveEnd,
            extension,
            List.copyOf(attention)
        );
    }

    private int countStatus(
        List<MateOperationRow> rows,
        MateStatus status
    ) {
        return (int) rows.stream()
            .filter(row -> status.name().equals(row.status()))
            .count();
    }
}
