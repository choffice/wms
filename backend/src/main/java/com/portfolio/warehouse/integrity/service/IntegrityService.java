package com.portfolio.warehouse.integrity.service;

import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.integrity.api.dto.*;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.log.service.BusinessAuditService;
import com.portfolio.warehouse.mate.domain.*;
import com.portfolio.warehouse.mate.repository.*;
import com.portfolio.warehouse.pda.domain.*;
import com.portfolio.warehouse.pda.repository.*;
import com.portfolio.warehouse.pda.service.PdaSessionService;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegrityService {

    public static final String RESET_ORPHAN_PDA_STATUS =
        "RESET_ORPHAN_PDA_STATUS";
    public static final String RESTORE_ACTIVE_PDA_STATUS =
        "RESTORE_ACTIVE_PDA_STATUS";
    public static final String RESET_STALE_WORKING_MATE =
        "RESET_STALE_WORKING_MATE";
    public static final String RESTORE_OPEN_SESSION_MATE_STATUS =
        "RESTORE_OPEN_SESSION_MATE_STATUS";
    public static final String RELEASE_OFF_DUTY_PDA =
        "RELEASE_OFF_DUTY_PDA";
    public static final String RESTORE_ASSIGNMENT_IN_PROGRESS =
        "RESTORE_ASSIGNMENT_IN_PROGRESS";

    private final PdaDeviceRepository deviceRepository;
    private final PdaUsageHistoryRepository usageRepository;
    private final MateRepository mateRepository;
    private final MateStatusHistoryRepository mateStatusHistoryRepository;
    private final WorkSessionRepository sessionRepository;
    private final WorkAssignmentRepository assignmentRepository;
    private final PdaSessionService pdaSessionService;
    private final BusinessAuditService auditService;

    public IntegrityService(
        PdaDeviceRepository deviceRepository,
        PdaUsageHistoryRepository usageRepository,
        MateRepository mateRepository,
        MateStatusHistoryRepository mateStatusHistoryRepository,
        WorkSessionRepository sessionRepository,
        WorkAssignmentRepository assignmentRepository,
        PdaSessionService pdaSessionService,
        BusinessAuditService auditService
    ) {
        this.deviceRepository = deviceRepository;
        this.usageRepository = usageRepository;
        this.mateRepository = mateRepository;
        this.mateStatusHistoryRepository = mateStatusHistoryRepository;
        this.sessionRepository = sessionRepository;
        this.assignmentRepository = assignmentRepository;
        this.pdaSessionService = pdaSessionService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public IntegrityScanResponse scan() {
        List<PdaDevice> devices = deviceRepository.findAll();

        List<PdaUsageHistory> activeUsages =
            usageRepository.findAll().stream()
                .filter(PdaUsageHistory::isActiveUsage)
                .toList();

        List<Mate> mates = mateRepository.findAll();
        List<WorkSession> openSessions =
            sessionRepository.findAllByEndedAtIsNull();

        Map<Long, List<PdaUsageHistory>> usagesByDevice =
            activeUsages.stream()
                .collect(
                    Collectors.groupingBy(
                        usage -> usage.getPdaDevice().getId()
                    )
                );

        Map<Long, List<PdaUsageHistory>> usagesByMate =
            activeUsages.stream()
                .collect(
                    Collectors.groupingBy(
                        usage -> usage.getMate().getId()
                    )
                );

        Map<Long, List<WorkSession>> sessionsByMate =
            openSessions.stream()
                .collect(
                    Collectors.groupingBy(
                        session -> session.getMate().getId()
                    )
                );

        Map<Long, List<WorkSession>> sessionsByAssignment =
            openSessions.stream()
                .collect(
                    Collectors.groupingBy(
                        session -> session
                            .getWorkAssignment()
                            .getId()
                    )
                );

        List<IntegrityIssueResponse> issues =
            new ArrayList<>();

        detectDuplicatePdaUsage(
            usagesByDevice,
            usagesByMate,
            issues
        );

        detectDuplicateOpenSessions(
            sessionsByMate,
            sessionsByAssignment,
            issues
        );

        detectPdaStateIssues(
            devices,
            usagesByDevice,
            issues
        );

        detectMateStateIssues(
            mates,
            usagesByMate,
            sessionsByMate,
            issues
        );

        detectSessionIssues(
            openSessions,
            sessionsByAssignment,
            issues
        );

        issues.sort(
            Comparator
                .<IntegrityIssueResponse>comparingInt(
                    issue -> severityRank(issue.severity())
                )
                .thenComparing(IntegrityIssueResponse::code)
                .thenComparing(
                    IntegrityIssueResponse::subject
                )
        );

        int critical =
            (int) issues.stream()
                .filter(issue ->
                    "CRITICAL".equals(issue.severity())
                )
                .count();

        int warning =
            (int) issues.stream()
                .filter(issue ->
                    "WARNING".equals(issue.severity())
                )
                .count();

        int repairable =
            (int) issues.stream()
                .filter(IntegrityIssueResponse::repairable)
                .count();

        return new IntegrityScanResponse(
            LocalDateTime.now(),
            new IntegritySummaryResponse(
                issues.size(),
                critical,
                warning,
                repairable
            ),
            List.copyOf(issues)
        );
    }

    @Transactional
    public IntegrityRepairResult repair(
        IntegrityRepairRequest request
    ) {
        boolean repaired =
            repairInternal(
                request.action(),
                request.entityId()
            );

        if (!repaired) {
            throw new BusinessException(
                "INTEGRITY_REPAIR_NOT_APPLICABLE",
                "현재 데이터 상태에서는 해당 안전복구를 적용할 수 없습니다. 다시 검사해주세요."
            );
        }

        return new IntegrityRepairResult(
            1,
            "안전복구를 적용했습니다."
        );
    }

    @Transactional
    public IntegrityRepairResult repairAllSafe() {
        IntegrityScanResponse current = scan();

        LinkedHashMap<String, IntegrityIssueResponse>
            uniqueRepairs = new LinkedHashMap<>();

        current.issues().stream()
            .filter(IntegrityIssueResponse::repairable)
            .forEach(issue ->
                uniqueRepairs.putIfAbsent(
                    issue.safeRepairAction()
                        + ":"
                        + issue.entityId(),
                    issue
                )
            );

        int repaired = 0;

        for (
            IntegrityIssueResponse issue :
            uniqueRepairs.values()
        ) {
            if (
                repairInternal(
                    issue.safeRepairAction(),
                    issue.entityId()
                )
            ) {
                repaired++;
            }
        }

        return new IntegrityRepairResult(
            repaired,
            repaired == 0
                ? "현재 적용 가능한 안전복구가 없습니다."
                : repaired
                    + "건의 안전복구를 적용했습니다."
        );
    }

    private void detectDuplicatePdaUsage(
        Map<Long, List<PdaUsageHistory>> usagesByDevice,
        Map<Long, List<PdaUsageHistory>> usagesByMate,
        List<IntegrityIssueResponse> issues
    ) {
        usagesByDevice.forEach((deviceId, usages) -> {
            if (usages.size() <= 1) return;

            Integer number =
                usages.get(0)
                    .getPdaDevice()
                    .getDeviceNumber();

            issues.add(
                issue(
                    "CRITICAL",
                    "DUPLICATE_ACTIVE_PDA_DEVICE",
                    "PDA_DEVICE",
                    deviceId,
                    "PDA " + number,
                    "동일 물리 PDA에 활성 사용이력 "
                        + usages.size()
                        + "건이 존재합니다.",
                    null
                )
            );
        });

        usagesByMate.forEach((mateId, usages) -> {
            if (usages.size() <= 1) return;

            Mate mate = usages.get(0).getMate();

            issues.add(
                issue(
                    "CRITICAL",
                    "DUPLICATE_ACTIVE_PDA_MATE",
                    "MATE",
                    mateId,
                    mate.getNickname(),
                    "동일 MATE에 활성 PDA 사용이력 "
                        + usages.size()
                        + "건이 존재합니다.",
                    null
                )
            );
        });
    }

    private void detectDuplicateOpenSessions(
        Map<Long, List<WorkSession>> sessionsByMate,
        Map<Long, List<WorkSession>> sessionsByAssignment,
        List<IntegrityIssueResponse> issues
    ) {
        sessionsByMate.forEach((mateId, sessions) -> {
            if (sessions.size() <= 1) return;

            Mate mate = sessions.get(0).getMate();

            issues.add(
                issue(
                    "CRITICAL",
                    "DUPLICATE_OPEN_SESSION_MATE",
                    "MATE",
                    mateId,
                    mate.getNickname(),
                    "동일 MATE에 Open WorkSession "
                        + sessions.size()
                        + "건이 존재합니다.",
                    null
                )
            );
        });

        sessionsByAssignment.forEach(
            (assignmentId, sessions) -> {
                if (sessions.size() <= 1) return;

                issues.add(
                    issue(
                        "CRITICAL",
                        "DUPLICATE_OPEN_SESSION_ASSIGNMENT",
                        "WORK_ASSIGNMENT",
                        assignmentId,
                        "Assignment #"
                            + assignmentId,
                        "동일 업무배정에 Open WorkSession "
                            + sessions.size()
                            + "건이 존재합니다.",
                        null
                    )
                );
            }
        );
    }

    private void detectPdaStateIssues(
        List<PdaDevice> devices,
        Map<Long, List<PdaUsageHistory>> usagesByDevice,
        List<IntegrityIssueResponse> issues
    ) {
        for (PdaDevice device : devices) {
            List<PdaUsageHistory> active =
                usagesByDevice.getOrDefault(
                    device.getId(),
                    List.of()
                );

            if (
                device.getStatus() == PdaStatus.IN_USE
                    && active.isEmpty()
            ) {
                issues.add(
                    issue(
                        "WARNING",
                        "PDA_IN_USE_WITHOUT_USAGE",
                        "PDA_DEVICE",
                        device.getId(),
                        "PDA " + device.getDeviceNumber(),
                        "기기 상태는 IN_USE지만 활성 사용이력이 없습니다.",
                        RESET_ORPHAN_PDA_STATUS
                    )
                );
            }

            if (active.size() != 1) {
                continue;
            }

            if (!device.isActive()) {
                issues.add(
                    issue(
                        "CRITICAL",
                        "INACTIVE_PDA_HAS_ACTIVE_USAGE",
                        "PDA_DEVICE",
                        device.getId(),
                        "PDA " + device.getDeviceNumber(),
                        "비활성 PDA에 활성 사용이력이 연결되어 있습니다.",
                        null
                    )
                );
                continue;
            }

            if (
                device.getStatus()
                    == PdaStatus.AVAILABLE
            ) {
                issues.add(
                    issue(
                        "WARNING",
                        "PDA_USAGE_STATUS_MISMATCH",
                        "PDA_DEVICE",
                        device.getId(),
                        "PDA " + device.getDeviceNumber(),
                        "활성 사용이력이 있지만 기기 상태가 AVAILABLE입니다.",
                        RESTORE_ACTIVE_PDA_STATUS
                    )
                );
            } else if (
                device.getStatus()
                    == PdaStatus.LOST
            ) {
                issues.add(
                    issue(
                        "WARNING",
                        "ACTIVE_PDA_MARKED_LOST",
                        "PDA_DEVICE",
                        device.getId(),
                        "PDA " + device.getDeviceNumber(),
                        "활성 사용 중인 PDA가 LOST 상태입니다. 실제 분실 여부를 확인해주세요.",
                        null
                    )
                );
            } else if (
                device.getStatus()
                    == PdaStatus.INSPECTION
                    || device.getStatus()
                        == PdaStatus.RETIRED
            ) {
                issues.add(
                    issue(
                        "CRITICAL",
                        "ACTIVE_USAGE_UNAVAILABLE_PDA",
                        "PDA_DEVICE",
                        device.getId(),
                        "PDA " + device.getDeviceNumber(),
                        "활성 사용이력이 있으나 기기 상태가 "
                            + device.getStatus()
                            + "입니다.",
                        null
                    )
                );
            }
        }
    }

    private void detectMateStateIssues(
        List<Mate> mates,
        Map<Long, List<PdaUsageHistory>> usagesByMate,
        Map<Long, List<WorkSession>> sessionsByMate,
        List<IntegrityIssueResponse> issues
    ) {
        for (Mate mate : mates) {
            List<WorkSession> sessions =
                sessionsByMate.getOrDefault(
                    mate.getId(),
                    List.of()
                );

            List<PdaUsageHistory> usages =
                usagesByMate.getOrDefault(
                    mate.getId(),
                    List.of()
                );

            if (!mate.isActive() && !sessions.isEmpty()) {
                issues.add(
                    issue(
                        "CRITICAL",
                        "INACTIVE_MATE_OPEN_SESSION",
                        "MATE",
                        mate.getId(),
                        mate.getNickname(),
                        "비활성 MATE에 Open WorkSession이 존재합니다.",
                        null
                    )
                );
            }

            if (
                sessions.isEmpty()
                    && mate.getCurrentStatus()
                        == MateStatus.WORKING
            ) {
                issues.add(
                    issue(
                        "WARNING",
                        "MATE_WORKING_WITHOUT_SESSION",
                        "MATE",
                        mate.getId(),
                        mate.getNickname(),
                        "MATE 상태는 WORKING이지만 Open WorkSession이 없습니다.",
                        RESET_STALE_WORKING_MATE
                    )
                );
            }

            if (
                sessions.size() == 1
                    && mate.isActive()
                    && mate.getCurrentStatus()
                        != MateStatus.WORKING
            ) {
                WorkSession session = sessions.get(0);

                issues.add(
                    issue(
                        "WARNING",
                        "OPEN_SESSION_MATE_STATUS_MISMATCH",
                        "MATE",
                        mate.getId(),
                        mate.getNickname(),
                        "Open WorkSession이 있지만 현재 상태가 "
                            + mate.getCurrentStatus()
                            + "입니다.",
                        safeSessionStructure(session)
                            ? RESTORE_OPEN_SESSION_MATE_STATUS
                            : null
                    )
                );
            }

            if (
                sessions.isEmpty()
                    && usages.size() == 1
                    && mate.getCurrentStatus()
                        == MateStatus.OFF_DUTY
            ) {
                PdaUsageHistory usage = usages.get(0);

                issues.add(
                    issue(
                        "WARNING",
                        "OFF_DUTY_WITH_ACTIVE_PDA",
                        "PDA_USAGE",
                        usage.getId(),
                        mate.getNickname(),
                        "퇴근 상태인데 PDA "
                            + usage
                                .getPdaDevice()
                                .getDeviceNumber()
                            + " 사용이력이 아직 열려 있습니다.",
                        usage.getPdaDevice().getStatus()
                                == PdaStatus.IN_USE
                            || usage.getPdaDevice().getStatus()
                                == PdaStatus.AVAILABLE
                            ? RELEASE_OFF_DUTY_PDA
                            : null
                    )
                );
            }
        }
    }

    private void detectSessionIssues(
        List<WorkSession> openSessions,
        Map<Long, List<WorkSession>> sessionsByAssignment,
        List<IntegrityIssueResponse> issues
    ) {
        for (WorkSession session : openSessions) {
            WorkAssignment assignment =
                session.getWorkAssignment();

            PdaUsageHistory usage =
                session.getPdaUsageHistory();

            if (
                assignment.getStatus()
                    == WorkAssignmentStatus.ASSIGNED
                && sessionsByAssignment
                    .getOrDefault(
                        assignment.getId(),
                        List.of()
                    )
                    .size() == 1
            ) {
                issues.add(
                    issue(
                        "WARNING",
                        "OPEN_SESSION_ASSIGNMENT_NOT_STARTED",
                        "WORK_ASSIGNMENT",
                        assignment.getId(),
                        "Assignment #"
                            + assignment.getId(),
                        "Open WorkSession이 있지만 Assignment 상태가 ASSIGNED입니다.",
                        RESTORE_ASSIGNMENT_IN_PROGRESS
                    )
                );
            }

            if (
                assignment.getStatus()
                    == WorkAssignmentStatus.COMPLETED
                    || assignment.getStatus()
                        == WorkAssignmentStatus.CANCELED
            ) {
                issues.add(
                    issue(
                        "CRITICAL",
                        "CLOSED_ASSIGNMENT_OPEN_SESSION",
                        "WORK_SESSION",
                        session.getId(),
                        "Session #"
                            + session.getId(),
                        "종료된 Assignment에 Open WorkSession이 남아 있습니다.",
                        null
                    )
                );
            }

            if (
                !assignment
                    .getCurrentMate()
                    .getId()
                    .equals(
                        session.getMate().getId()
                    )
            ) {
                issues.add(
                    issue(
                        "CRITICAL",
                        "SESSION_ASSIGNMENT_MATE_MISMATCH",
                        "WORK_SESSION",
                        session.getId(),
                        "Session #"
                            + session.getId(),
                        "세션 수행 MATE와 Assignment 현재 담당자가 다릅니다.",
                        null
                    )
                );
            }

            if (!usage.isActiveUsage()) {
                issues.add(
                    issue(
                        "CRITICAL",
                        "OPEN_SESSION_RELEASED_PDA_USAGE",
                        "WORK_SESSION",
                        session.getId(),
                        "Session #"
                            + session.getId(),
                        "Open WorkSession이 이미 반납된 PDA 사용이력을 참조합니다.",
                        null
                    )
                );
            }

            if (
                !usage.getMate()
                    .getId()
                    .equals(
                        session.getMate().getId()
                    )
            ) {
                issues.add(
                    issue(
                        "CRITICAL",
                        "SESSION_PDA_MATE_MISMATCH",
                        "WORK_SESSION",
                        session.getId(),
                        "Session #"
                            + session.getId(),
                        "WorkSession MATE와 PDA 사용이력 MATE가 다릅니다.",
                        null
                    )
                );
            }
        }
    }

    private boolean repairInternal(
        String action,
        Long entityId
    ) {
        return switch (action) {
            case RESET_ORPHAN_PDA_STATUS ->
                resetOrphanPdaStatus(entityId);

            case RESTORE_ACTIVE_PDA_STATUS ->
                restoreActivePdaStatus(entityId);

            case RESET_STALE_WORKING_MATE ->
                resetStaleWorkingMate(entityId);

            case RESTORE_OPEN_SESSION_MATE_STATUS ->
                restoreSessionMateStatus(entityId);

            case RELEASE_OFF_DUTY_PDA ->
                releaseOffDutyPda(entityId);

            case RESTORE_ASSIGNMENT_IN_PROGRESS ->
                restoreAssignmentInProgress(entityId);

            default -> throw new BusinessException(
                "INTEGRITY_REPAIR_UNKNOWN",
                "지원하지 않는 안전복구 작업입니다."
            );
        };
    }

    private boolean resetOrphanPdaStatus(
        Long deviceId
    ) {
        PdaDevice device =
            deviceRepository
                .findByIdForUpdate(deviceId)
                .orElseThrow(() ->
                    new NotFoundException(
                        "PDA_NOT_FOUND",
                        "PDA 기기를 찾을 수 없습니다."
                    )
                );

        boolean hasUsage =
            usageRepository
                .findFirstByPdaDeviceIdAndReleasedAtIsNull(
                    deviceId
                )
                .isPresent();

        if (
            device.getStatus() != PdaStatus.IN_USE
                || hasUsage
        ) {
            return false;
        }

        device.changeStatus(PdaStatus.AVAILABLE);

        auditService.record(
            ActivityType.INTEGRITY_REPAIR,
            "PDA " + device.getDeviceNumber(),
            "정합성 복구 · IN_USE → AVAILABLE / 활성 사용이력 없음",
            "PDA_DEVICE",
            device.getId()
        );

        return true;
    }

    private boolean restoreActivePdaStatus(
        Long deviceId
    ) {
        PdaDevice device =
            deviceRepository
                .findByIdForUpdate(deviceId)
                .orElseThrow(() ->
                    new NotFoundException(
                        "PDA_NOT_FOUND",
                        "PDA 기기를 찾을 수 없습니다."
                    )
                );

        List<PdaUsageHistory> activeUsages =
            usageRepository.findAll().stream()
                .filter(PdaUsageHistory::isActiveUsage)
                .filter(usage ->
                    usage.getPdaDevice()
                        .getId()
                        .equals(deviceId)
                )
                .toList();

        if (
            activeUsages.size() != 1
                || device.getStatus()
                    != PdaStatus.AVAILABLE
        ) {
            return false;
        }

        device.changeStatus(PdaStatus.IN_USE);

        auditService.record(
            ActivityType.INTEGRITY_REPAIR,
            "PDA " + device.getDeviceNumber(),
            "정합성 복구 · AVAILABLE → IN_USE / 활성 사용이력 존재",
            "PDA_DEVICE",
            device.getId()
        );

        return true;
    }

    private boolean resetStaleWorkingMate(
        Long mateId
    ) {
        Mate mate =
            mateRepository
                .findByIdForUpdate(mateId)
                .orElseThrow(() ->
                    new NotFoundException(
                        "MATE_NOT_FOUND",
                        "MATE를 찾을 수 없습니다."
                    )
                );

        boolean hasOpenSession =
            sessionRepository
                .findFirstByMateIdAndEndedAtIsNull(
                    mateId
                )
                .isPresent();

        if (
            !mate.isActive()
                || hasOpenSession
                || mate.getCurrentStatus()
                    != MateStatus.WORKING
        ) {
            return false;
        }

        mate.changeStatus(
            MateStatus.AVAILABLE,
            "대기"
        );

        mateStatusHistoryRepository.save(
            new MateStatusHistory(
                mate,
                MateStatus.AVAILABLE,
                "대기"
            )
        );

        auditService.record(
            ActivityType.INTEGRITY_REPAIR,
            mate.getNickname(),
            "정합성 복구 · WORKING → AVAILABLE / Open Session 없음",
            "MATE_STATUS",
            mate.getId()
        );

        return true;
    }

    private boolean restoreSessionMateStatus(
        Long mateId
    ) {
        Mate mate =
            mateRepository
                .findByIdForUpdate(mateId)
                .orElseThrow(() ->
                    new NotFoundException(
                        "MATE_NOT_FOUND",
                        "MATE를 찾을 수 없습니다."
                    )
                );

        List<WorkSession> sessions =
            sessionRepository
                .findAllByEndedAtIsNull()
                .stream()
                .filter(session ->
                    session.getMate()
                        .getId()
                        .equals(mateId)
                )
                .toList();

        if (
            !mate.isActive()
                || sessions.size() != 1
                || mate.getCurrentStatus()
                    == MateStatus.WORKING
                || !safeSessionStructure(
                    sessions.get(0)
                )
        ) {
            return false;
        }

        MateStatus before =
            mate.getCurrentStatus();

        mate.changeStatus(
            MateStatus.WORKING,
            "업무중"
        );

        mateStatusHistoryRepository.save(
            new MateStatusHistory(
                mate,
                MateStatus.WORKING,
                "업무중"
            )
        );

        auditService.record(
            ActivityType.INTEGRITY_REPAIR,
            mate.getNickname(),
            "정합성 복구 · "
                + before
                + " → WORKING / Open Session 존재",
            "MATE_STATUS",
            mate.getId()
        );

        return true;
    }

    private boolean releaseOffDutyPda(
        Long usageId
    ) {
        PdaUsageHistory usage =
            usageRepository.findById(usageId)
                .orElseThrow(() ->
                    new NotFoundException(
                        "PDA_USAGE_NOT_FOUND",
                        "PDA 사용 이력을 찾을 수 없습니다."
                    )
                );

        if (!usage.isActiveUsage()) {
            return false;
        }

        Mate mate = usage.getMate();

        boolean hasOpenSession =
            sessionRepository
                .findFirstByMateIdAndEndedAtIsNull(
                    mate.getId()
                )
                .isPresent();

        long activeUsageCount =
            usageRepository.findAll().stream()
                .filter(PdaUsageHistory::isActiveUsage)
                .filter(item ->
                    item.getMate()
                        .getId()
                        .equals(mate.getId())
                )
                .count();

        PdaStatus deviceStatus =
            usage.getPdaDevice().getStatus();

        if (
            hasOpenSession
                || activeUsageCount != 1
                || mate.getCurrentStatus()
                    != MateStatus.OFF_DUTY
                || (
                    deviceStatus != PdaStatus.IN_USE
                        && deviceStatus
                            != PdaStatus.AVAILABLE
                )
        ) {
            return false;
        }

        pdaSessionService.releaseByAdmin(
            usageId,
            PdaReleaseReason.ADMIN_RELEASE
        );

        auditService.record(
            ActivityType.INTEGRITY_REPAIR,
            mate.getNickname(),
            "정합성 복구 · 퇴근 후 미반납 PDA "
                + usage
                    .getPdaDevice()
                    .getDeviceNumber()
                + " 관리자 회수",
            "PDA_USAGE",
            usageId
        );

        return true;
    }

    private boolean restoreAssignmentInProgress(
        Long assignmentId
    ) {
        WorkAssignment assignment =
            assignmentRepository
                .findByIdForUpdate(assignmentId)
                .orElseThrow(() ->
                    new NotFoundException(
                        "ASSIGNMENT_NOT_FOUND",
                        "업무배정을 찾을 수 없습니다."
                    )
                );

        List<WorkSession> openSessions =
            sessionRepository
                .findAllByEndedAtIsNull()
                .stream()
                .filter(session ->
                    session.getWorkAssignment()
                        .getId()
                        .equals(assignmentId)
                )
                .toList();

        if (
            openSessions.size() != 1
                || assignment.getStatus()
                    != WorkAssignmentStatus.ASSIGNED
                || !safeSessionStructure(
                    openSessions.get(0)
                )
        ) {
            return false;
        }

        assignment.start();

        auditService.record(
            ActivityType.INTEGRITY_REPAIR,
            "Assignment #"
                + assignmentId,
            "정합성 복구 · ASSIGNED → IN_PROGRESS / Open Session 존재",
            "WORK_ASSIGNMENT",
            assignmentId
        );

        return true;
    }

    private boolean safeSessionStructure(
        WorkSession session
    ) {
        WorkAssignment assignment =
            session.getWorkAssignment();

        PdaUsageHistory usage =
            session.getPdaUsageHistory();

        return assignment.getStatus()
                != WorkAssignmentStatus.COMPLETED
            && assignment.getStatus()
                != WorkAssignmentStatus.CANCELED
            && assignment
                .getCurrentMate()
                .getId()
                .equals(session.getMate().getId())
            && usage.isActiveUsage()
            && usage
                .getMate()
                .getId()
                .equals(session.getMate().getId());
    }

    private IntegrityIssueResponse issue(
        String severity,
        String code,
        String entityType,
        Long entityId,
        String subject,
        String detail,
        String safeRepairAction
    ) {
        return new IntegrityIssueResponse(
            code
                + ":"
                + entityType
                + ":"
                + entityId,
            severity,
            code,
            entityType,
            entityId,
            subject,
            detail,
            safeRepairAction
        );
    }

    private int severityRank(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 0;
            case "WARNING" -> 1;
            default -> 2;
        };
    }
}
