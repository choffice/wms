package com.portfolio.warehouse.work.service;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.common.event.OperationalEventService;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.location.domain.Location;
import com.portfolio.warehouse.location.repository.LocationRepository;
import com.portfolio.warehouse.mate.domain.*;
import com.portfolio.warehouse.mate.repository.MateRepository;
import com.portfolio.warehouse.mate.repository.MateStatusHistoryRepository;
import com.portfolio.warehouse.mate.service.WorkScheduleResolver;
import com.portfolio.warehouse.pda.domain.PdaUsageHistory;
import com.portfolio.warehouse.pda.service.PdaSessionService;
import com.portfolio.warehouse.work.api.dto.*;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkAssignmentService {

    private final WorkAssignmentRepository assignmentRepository;
    private final WorkTypeRepository workTypeRepository;
    private final WorkProgressRepository progressRepository;
    private final WorkSessionRepository sessionRepository;
    private final WorkAssignmentHistoryRepository historyRepository;
    private final LocationRepository locationRepository;
    private final MateRepository mateRepository;
    private final MateStatusHistoryRepository mateStatusHistoryRepository;
    private final CurrentUserService currentUserService;
    private final PdaSessionService pdaSessionService;
    private final OperationalEventService eventService;
    private final WorkScheduleResolver scheduleResolver;

    public WorkAssignmentService(
        WorkAssignmentRepository assignmentRepository,
        WorkTypeRepository workTypeRepository,
        WorkProgressRepository progressRepository,
        WorkSessionRepository sessionRepository,
        WorkAssignmentHistoryRepository historyRepository,
        LocationRepository locationRepository,
        MateRepository mateRepository,
        MateStatusHistoryRepository mateStatusHistoryRepository,
        CurrentUserService currentUserService,
        PdaSessionService pdaSessionService,
        OperationalEventService eventService,
        WorkScheduleResolver scheduleResolver
    ) {
        this.assignmentRepository = assignmentRepository;
        this.workTypeRepository = workTypeRepository;
        this.progressRepository = progressRepository;
        this.sessionRepository = sessionRepository;
        this.historyRepository = historyRepository;
        this.locationRepository = locationRepository;
        this.mateRepository = mateRepository;
        this.mateStatusHistoryRepository = mateStatusHistoryRepository;
        this.currentUserService = currentUserService;
        this.pdaSessionService = pdaSessionService;
        this.eventService = eventService;
        this.scheduleResolver = scheduleResolver;
    }

    @Transactional
    public WorkAssignmentResponse assign(WorkAssignmentCreateRequest request) {
        UserAccount admin = currentUserService.account();

        WorkType workType = workTypeRepository.findById(request.workTypeId())
            .orElseThrow(() -> new NotFoundException("WORK_TYPE_NOT_FOUND", "업무 종류를 찾을 수 없습니다."));

        if (!workType.isActive()) {
            throw new BusinessException("WORK_TYPE_INACTIVE", "비활성 업무 종류는 배정할 수 없습니다.");
        }

        Location area = location(request.areaLocationId());
        Location start = location(request.startLocationId());
        Mate mate = mate(request.mateId());

        if (!mate.isActive()) {
            throw new BusinessException("MATE_INACTIVE", "비활성 MATE에게 업무를 배정할 수 없습니다.");
        }

        requireWithinArea(area, start);

        WorkAssignment assignment = assignmentRepository.save(
            new WorkAssignment(workType, area, start, mate, admin)
        );

        historyRepository.save(
            new WorkAssignmentHistory(
                assignment,
                WorkAssignmentActionType.ASSIGN,
                null,
                mate,
                admin,
                null
            )
        );

        eventService.publish(
            ActivityType.WORK_ASSIGN,
            admin,
            mate.getNickname(),
            workType.getName() + ":" + area.getFullCode() + " 시작",
            "WORK_ASSIGNMENT",
            assignment.getId(),
            true,
            true
        );

        return WorkAssignmentResponse.from(assignment);
    }

    @Transactional(readOnly = true)
    public List<WorkAssignmentResponse> adminList() {
        return assignmentRepository.findAllByOrderByAssignedAtDesc().stream()
            .map(WorkAssignmentResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkAssignmentResponse> myAssignments() {
        Mate mate = currentUserService.mate();
        return assignmentRepository.findAllByCurrentMateIdAndStatusInOrderByAssignedAtDesc(
                mate.getId(),
                List.of(
                    WorkAssignmentStatus.ASSIGNED,
                    WorkAssignmentStatus.IN_PROGRESS,
                    WorkAssignmentStatus.COMPLETED
                )
            ).stream()
            .map(WorkAssignmentResponse::from)
            .toList();
    }

    @Transactional
    public WorkSessionResponse start(Long assignmentId) {
        Mate mate = currentUserService.mate();
        WorkAssignment assignment = assignmentRepository.findByIdForUpdate(assignmentId)
            .orElseThrow(() -> new NotFoundException("ASSIGNMENT_NOT_FOUND", "업무배정을 찾을 수 없습니다."));

        requireCurrentMate(assignment, mate);
        requireOpenable(assignment);

        sessionRepository.findFirstByMateIdAndEndedAtIsNull(mate.getId())
            .ifPresent(session -> {
                throw new BusinessException(
                    "ACTIVE_WORK_EXISTS",
                    "현재 진행 중인 업무를 먼저 일시정지 또는 종료해주세요."
                );
            });

        PdaUsageHistory pdaUsage = pdaSessionService.activeUsageForMate(mate.getId());
        LocalDateTime now = LocalDateTime.now();

        assignment.start();
        java.time.LocalDate shiftDate =
            scheduleResolver.resolveShiftDate(
                mate.getId(),
                now
            );

        WorkSession session = sessionRepository.save(
            new WorkSession(
                assignment,
                mate,
                pdaUsage,
                shiftDate,
                now
            )
        );

        changeMateStatus(mate, MateStatus.WORKING, assignment.getAreaLocation().getFullCode());
        return WorkSessionResponse.from(session);
    }

    @Transactional
    public WorkProgressResponse progress(Long assignmentId, WorkProgressRequest request) {
        Mate mate = currentUserService.mate();
        WorkAssignment assignment = assignmentRepository.findByIdForUpdate(assignmentId)
            .orElseThrow(() -> new NotFoundException("ASSIGNMENT_NOT_FOUND", "업무배정을 찾을 수 없습니다."));

        requireCurrentMate(assignment, mate);

        if (assignment.getStatus() == WorkAssignmentStatus.CANCELED) {
            throw new BusinessException("ASSIGNMENT_CANCELED", "취소된 업무는 기록할 수 없습니다.");
        }

        requireExpectedProgress(
            assignment,
            request.expectedCurrentLocationId()
        );

        Location newLocation = location(request.lastCompletedLocationId());
        requireWithinArea(assignment.getAreaLocation(), newLocation);
        requireNotBeforeStart(assignment, newLocation);

        Location previous = assignment.getCurrentLastCompletedLocation();
        boolean correction = previous != null
            && (assignment.getStatus() == WorkAssignmentStatus.COMPLETED
                || newLocation.getFullCode().compareTo(previous.getFullCode()) < 0);

        WorkProgress progress = progressRepository.save(
            new WorkProgress(
                assignment,
                mate,
                mate.getAccount(),
                newLocation,
                previous,
                correction,
                trimToNull(request.reason())
            )
        );

        assignment.updateLastCompletedLocation(newLocation);

        eventService.publish(
            ActivityType.WORK_PROGRESS,
            mate.getAccount(),
            "관리자",
            assignment.getWorkType().getName() + ":" + newLocation.getFullCode() + "까지 진행",
            "WORK_ASSIGNMENT",
            assignment.getId(),
            true,
            false
        );

        return WorkProgressResponse.from(progress);
    }

    @Transactional
    public WorkSessionResponse pause(Long assignmentId, WorkPauseRequest request) {
        Mate mate = currentUserService.mate();
        WorkAssignment assignment = assignmentRepository.findByIdForUpdate(assignmentId)
            .orElseThrow(() -> new NotFoundException("ASSIGNMENT_NOT_FOUND", "업무배정을 찾을 수 없습니다."));

        requireCurrentMate(assignment, mate);

        WorkSession session = sessionRepository.findFirstByWorkAssignmentIdAndEndedAtIsNull(assignmentId)
            .orElseThrow(() -> new BusinessException("NO_ACTIVE_SESSION", "진행 중인 작업 세션이 없습니다."));

        session.close(LocalDateTime.now(), WorkSessionEndReason.PAUSED);

        MateStatus nextStatus = request == null || request.nextStatus() == null
            ? MateStatus.AVAILABLE
            : request.nextStatus();

        if (nextStatus == MateStatus.WORKING) {
            throw new BusinessException("INVALID_PAUSE_STATUS", "일시정지 후 상태는 WORKING일 수 없습니다.");
        }

        String whereabouts = request == null ? null : trimToNull(request.whereabouts());
        changeMateStatus(
            mate,
            nextStatus,
            whereabouts == null ? defaultWhereabouts(nextStatus) : whereabouts
        );

        return WorkSessionResponse.from(session);
    }

    @Transactional
    public WorkSessionResponse resume(Long assignmentId) {
        return start(assignmentId);
    }

    @Transactional
    public WorkAssignmentResponse complete(Long assignmentId, WorkCompleteRequest request) {
        Mate mate = currentUserService.mate();
        UserAccount account = currentUserService.account();

        WorkAssignment assignment = assignmentRepository.findByIdForUpdate(assignmentId)
            .orElseThrow(() -> new NotFoundException("ASSIGNMENT_NOT_FOUND", "업무배정을 찾을 수 없습니다."));

        requireCurrentMate(assignment, mate);

        if (assignment.getStatus() == WorkAssignmentStatus.COMPLETED) {
            return WorkAssignmentResponse.from(assignment);
        }

        if (request != null && request.lastCompletedLocationId() != null) {
            requireExpectedProgress(
                assignment,
                request.expectedCurrentLocationId()
            );

            recordProgressInternal(
                assignment,
                mate,
                request.lastCompletedLocationId(),
                request.correctionReason()
            );
        }

        sessionRepository.findFirstByWorkAssignmentIdAndEndedAtIsNull(assignmentId)
            .ifPresent(session -> session.close(LocalDateTime.now(), WorkSessionEndReason.COMPLETED));

        LocalDateTime now = LocalDateTime.now();
        assignment.complete(now);

        historyRepository.save(
            new WorkAssignmentHistory(
                assignment,
                WorkAssignmentActionType.COMPLETE,
                mate,
                mate,
                account,
                null
            )
        );

        changeMateStatus(mate, MateStatus.AVAILABLE, "대기");

        eventService.publish(
            ActivityType.WORK_COMPLETE,
            mate.getAccount(),
            "관리자",
            assignment.getWorkType().getName()
                + ":"
                + (assignment.getCurrentLastCompletedLocation() == null
                    ? assignment.getStartLocation().getFullCode()
                    : assignment.getCurrentLastCompletedLocation().getFullCode())
                + " 완료",
            "WORK_ASSIGNMENT",
            assignment.getId(),
            true,
            false
        );

        return WorkAssignmentResponse.from(assignment);
    }

    @Transactional
    public WorkAssignmentResponse trade(Long assignmentId, WorkAssignmentTradeRequest request) {
        UserAccount admin = currentUserService.account();
        WorkAssignment assignment = assignmentRepository.findByIdForUpdate(assignmentId)
            .orElseThrow(() -> new NotFoundException("ASSIGNMENT_NOT_FOUND", "업무배정을 찾을 수 없습니다."));

        if (assignment.getStatus() == WorkAssignmentStatus.COMPLETED
            || assignment.getStatus() == WorkAssignmentStatus.CANCELED) {
            throw new BusinessException("ASSIGNMENT_CLOSED", "종료된 업무는 트레이드할 수 없습니다.");
        }

        if (
            request.expectedCurrentMateId() != null
                && !assignment
                    .getCurrentMate()
                    .getId()
                    .equals(request.expectedCurrentMateId())
        ) {
            throw new BusinessException(
                "ASSIGNMENT_STALE_MATE",
                "담당 MATE가 다른 요청으로 변경되었습니다. 최신 업무배정을 다시 확인해주세요."
            );
        }

        if (sessionRepository.findFirstByWorkAssignmentIdAndEndedAtIsNull(assignmentId).isPresent()) {
            throw new BusinessException(
                "ASSIGNMENT_ACTIVE_SESSION",
                "진행 중인 작업 세션을 먼저 일시정지한 뒤 트레이드해주세요."
            );
        }

        Mate fromMate = assignment.getCurrentMate();
        Mate toMate = mate(request.toMateId());

        if (!toMate.isActive()) {
            throw new BusinessException("MATE_INACTIVE", "비활성 MATE에게 트레이드할 수 없습니다.");
        }

        assignment.tradeTo(toMate);

        historyRepository.save(
            new WorkAssignmentHistory(
                assignment,
                WorkAssignmentActionType.TRADE,
                fromMate,
                toMate,
                admin,
                trimToNull(request.reason())
            )
        );

        eventService.publish(
            ActivityType.WORK_TRADE,
            admin,
            toMate.getNickname(),
            assignment.getWorkType().getName()
                + ":"
                + assignment.getAreaLocation().getFullCode()
                + " 담당 "
                + fromMate.getNickname()
                + " → "
                + toMate.getNickname(),
            "WORK_ASSIGNMENT",
            assignment.getId(),
            true,
            true
        );

        return WorkAssignmentResponse.from(assignment);
    }

@Transactional
public WorkAssignmentResponse cancel(
    Long assignmentId,
    WorkAssignmentCancelRequest request
) {
    UserAccount admin = currentUserService.account();

    WorkAssignment assignment = assignmentRepository
        .findByIdForUpdate(assignmentId)
        .orElseThrow(() -> new NotFoundException(
            "ASSIGNMENT_NOT_FOUND",
            "업무배정을 찾을 수 없습니다."
        ));

    if (assignment.getStatus() == WorkAssignmentStatus.COMPLETED
        || assignment.getStatus() == WorkAssignmentStatus.CANCELED) {
        throw new BusinessException(
            "ASSIGNMENT_CLOSED",
            "이미 종료된 업무입니다."
        );
    }

    if (sessionRepository
        .findFirstByWorkAssignmentIdAndEndedAtIsNull(assignmentId)
        .isPresent()) {
        throw new BusinessException(
            "ASSIGNMENT_ACTIVE_SESSION",
            "현재 작업 중인 세션을 먼저 일시정지한 뒤 취소해주세요."
        );
    }

    Mate currentMate = assignment.getCurrentMate();
    String reason = request == null
        ? null
        : trimToNull(request.reason());

    assignment.cancel();

    historyRepository.save(
        new WorkAssignmentHistory(
            assignment,
            WorkAssignmentActionType.CANCEL,
            currentMate,
            null,
            admin,
            reason
        )
    );

    eventService.publish(
        ActivityType.WORK_CANCEL,
        admin,
        currentMate.getNickname(),
        assignment.getWorkType().getName()
            + ":"
            + assignment.getAreaLocation().getFullCode()
            + " 배정 취소",
        "WORK_ASSIGNMENT",
        assignment.getId(),
        true,
        true
    );

    return WorkAssignmentResponse.from(assignment);
}

    @Transactional
    public WorkProgressResponse adminCorrectProgress(
        Long assignmentId,
        AdminWorkProgressCorrectionRequest request
    ) {
        UserAccount admin = currentUserService.account();

        WorkAssignment assignment =
            assignmentRepository
                .findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new NotFoundException(
                    "ASSIGNMENT_NOT_FOUND",
                    "업무배정을 찾을 수 없습니다."
                ));

        if (
            assignment.getStatus()
                == WorkAssignmentStatus.CANCELED
        ) {
            throw new BusinessException(
                "ASSIGNMENT_CANCELED",
                "취소된 업무의 진행위치는 정정할 수 없습니다."
            );
        }

        if (
            sessionRepository
                .findFirstByWorkAssignmentIdAndEndedAtIsNull(
                    assignmentId
                )
                .isPresent()
        ) {
            throw new BusinessException(
                "CORRECTION_ACTIVE_SESSION",
                "현재 작업 중인 세션이 있습니다. 작업을 일시정지한 뒤 정정해주세요."
            );
        }

        Location current =
            assignment.getCurrentLastCompletedLocation();

        Long actualCurrentId =
            current == null
                ? null
                : current.getId();

        if (
            !java.util.Objects.equals(
                actualCurrentId,
                request.expectedCurrentLocationId()
            )
        ) {
            throw new BusinessException(
                "PROGRESS_STALE",
                "진행위치가 다른 요청으로 변경되었습니다. 최신 이력을 다시 확인한 뒤 정정해주세요."
            );
        }

        Location corrected =
            location(request.correctedLocationId());

        requireWithinArea(
            assignment.getAreaLocation(),
            corrected
        );
        requireNotBeforeStart(
            assignment,
            corrected
        );

        if (
            current != null
                && current.getId()
                    .equals(corrected.getId())
        ) {
            throw new BusinessException(
                "PROGRESS_SAME_LOCATION",
                "현재 마지막 수행위치와 동일합니다."
            );
        }

        Mate performedMate =
            assignment.getCurrentMate();

        WorkProgress progress =
            progressRepository.save(
                new WorkProgress(
                    assignment,
                    performedMate,
                    admin,
                    corrected,
                    current,
                    true,
                    trimToNull(request.reason())
                )
            );

        assignment.updateLastCompletedLocation(
            corrected
        );

        eventService.publish(
            ActivityType.WORK_PROGRESS_CORRECTION,
            admin,
            performedMate.getNickname(),
            "Assignment #"
                + assignmentId
                + " 진행위치 정정 · "
                + (
                    current == null
                        ? "미기록"
                        : current.getFullCode()
                )
                + " → "
                + corrected.getFullCode(),
            "WORK_ASSIGNMENT",
            assignmentId,
            true,
            true
        );

        return WorkProgressResponse.from(progress);
    }

    @Transactional
    public WorkProgressResponse undoLatestProgressCorrection(
        Long assignmentId,
        AdminUndoProgressCorrectionRequest request
    ) {
        UserAccount admin =
            currentUserService.account();

        WorkAssignment assignment =
            assignmentRepository
                .findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new NotFoundException(
                    "ASSIGNMENT_NOT_FOUND",
                    "업무배정을 찾을 수 없습니다."
                ));

        if (
            assignment.getStatus()
                == WorkAssignmentStatus.CANCELED
        ) {
            throw new BusinessException(
                "ASSIGNMENT_CANCELED",
                "취소된 업무의 진행위치 정정은 되돌릴 수 없습니다."
            );
        }

        if (
            sessionRepository
                .findFirstByWorkAssignmentIdAndEndedAtIsNull(
                    assignmentId
                )
                .isPresent()
        ) {
            throw new BusinessException(
                "CORRECTION_ACTIVE_SESSION",
                "현재 작업 중인 세션이 있습니다. 작업을 일시정지한 뒤 정정 이력을 되돌려주세요."
            );
        }

        WorkProgress latest =
            progressRepository
                .findFirstByWorkAssignmentIdOrderByReportedAtDescIdDesc(
                    assignmentId
                )
                .orElseThrow(() -> new BusinessException(
                    "NO_PROGRESS_HISTORY",
                    "되돌릴 진행기록이 없습니다."
                ));

        if (
            !latest.getId()
                .equals(request.expectedLatestProgressId())
        ) {
            throw new BusinessException(
                "PROGRESS_STALE",
                "최근 진행기록이 다른 요청으로 변경되었습니다. 최신 이력을 다시 확인해주세요."
            );
        }

        if (!latest.isCorrection()) {
            throw new BusinessException(
                "LATEST_PROGRESS_NOT_CORRECTION",
                "가장 최근 기록이 정정 이력이 아니므로 자동 되돌리기를 할 수 없습니다."
            );
        }

        Location previous =
            latest.getPreviousLocation();

        if (previous == null) {
            throw new BusinessException(
                "CORRECTION_UNDO_NO_PREVIOUS",
                "정정 전 위치가 없는 기록은 자동 되돌리기를 지원하지 않습니다."
            );
        }

        Location current =
            assignment.getCurrentLastCompletedLocation();

        if (
            current == null
                || !current.getId()
                    .equals(
                        request.expectedCurrentLocationId()
                    )
                || !current.getId()
                    .equals(
                        latest
                            .getLastCompletedLocation()
                            .getId()
                    )
        ) {
            throw new BusinessException(
                "PROGRESS_STALE",
                "현재 진행위치가 정정 당시 값과 달라졌습니다. 최신 이력을 다시 확인해주세요."
            );
        }

        Mate performedMate =
            assignment.getCurrentMate();

        String reason =
            trimToNull(request.reason());

        if (reason == null) {
            reason =
                "최근 정정 되돌리기 · progress #"
                    + latest.getId();
        }

        WorkProgress undo =
            progressRepository.save(
                new WorkProgress(
                    assignment,
                    performedMate,
                    admin,
                    previous,
                    current,
                    true,
                    reason
                )
            );

        assignment.updateLastCompletedLocation(
            previous
        );

        eventService.publish(
            ActivityType.WORK_PROGRESS_CORRECTION,
            admin,
            performedMate.getNickname(),
            "Assignment #"
                + assignmentId
                + " 최근 정정 되돌리기 · "
                + current.getFullCode()
                + " → "
                + previous.getFullCode(),
            "WORK_ASSIGNMENT",
            assignmentId,
            true,
            true
        );

        return WorkProgressResponse.from(undo);
    }

    @Transactional(readOnly = true)
    public List<WorkProgressResponse> progressHistory(Long assignmentId) {
        return progressRepository.findAllByWorkAssignmentIdOrderByReportedAtAsc(assignmentId).stream()
            .map(WorkProgressResponse::from)
            .toList();
    }

@Transactional(readOnly = true)
public List<WorkAssignmentHistoryResponse> assignmentHistory(
    Long assignmentId
) {
    return historyRepository
        .findAllByWorkAssignmentIdOrderByChangedAtAsc(assignmentId)
        .stream()
        .map(WorkAssignmentHistoryResponse::from)
        .toList();
}

    @Transactional(readOnly = true)
    public List<WorkSessionResponse> sessionHistory(Long assignmentId) {
        return sessionRepository.findAllByWorkAssignmentIdOrderByStartedAtAsc(assignmentId).stream()
            .map(WorkSessionResponse::from)
            .toList();
    }

    private WorkProgress recordProgressInternal(
        WorkAssignment assignment,
        Mate mate,
        Long newLocationId,
        String reason
    ) {
        Location newLocation = location(newLocationId);
        requireWithinArea(assignment.getAreaLocation(), newLocation);
        requireNotBeforeStart(assignment, newLocation);

        Location previous = assignment.getCurrentLastCompletedLocation();
        boolean correction = previous != null
            && newLocation.getFullCode().compareTo(previous.getFullCode()) < 0;

        WorkProgress progress = progressRepository.save(
            new WorkProgress(
                assignment,
                mate,
                mate.getAccount(),
                newLocation,
                previous,
                correction,
                trimToNull(reason)
            )
        );

        assignment.updateLastCompletedLocation(newLocation);
        return progress;
    }

    private void requireExpectedProgress(
        WorkAssignment assignment,
        Long expectedCurrentLocationId
    ) {
        if (expectedCurrentLocationId == null) {
            if (
                assignment.getCurrentLastCompletedLocation()
                    != null
            ) {
                throw new BusinessException(
                    "PROGRESS_STALE",
                    "마지막 수행위치가 다른 요청으로 변경되었습니다. 최신 값을 다시 확인해주세요."
                );
            }
            return;
        }

        Location current =
            assignment.getCurrentLastCompletedLocation();

        if (
            current == null
                || !current.getId()
                    .equals(expectedCurrentLocationId)
        ) {
            throw new BusinessException(
                "PROGRESS_STALE",
                "마지막 수행위치가 다른 요청으로 변경되었습니다. 최신 값을 다시 확인해주세요."
            );
        }
    }

    private void requireCurrentMate(WorkAssignment assignment, Mate mate) {
        if (!assignment.getCurrentMate().getId().equals(mate.getId())) {
            throw new BusinessException("NOT_ASSIGNED_MATE", "현재 담당 MATE가 아닙니다.");
        }
    }

    private void requireOpenable(WorkAssignment assignment) {
        if (assignment.getStatus() == WorkAssignmentStatus.COMPLETED
            || assignment.getStatus() == WorkAssignmentStatus.CANCELED) {
            throw new BusinessException("ASSIGNMENT_CLOSED", "종료된 업무는 시작할 수 없습니다.");
        }

        if (sessionRepository.findFirstByWorkAssignmentIdAndEndedAtIsNull(assignment.getId()).isPresent()) {
            throw new BusinessException("SESSION_ALREADY_OPEN", "이미 진행 중인 작업 세션입니다.");
        }
    }

    private void requireWithinArea(Location area, Location location) {
        String areaCode = area.getFullCode();
        String code = location.getFullCode();

        if (!code.equals(areaCode) && !code.startsWith(areaCode + "-")) {
            throw new BusinessException(
                "LOCATION_OUTSIDE_AREA",
                "선택한 로케이션이 배정 구역에 포함되지 않습니다."
            );
        }
    }

    private void requireNotBeforeStart(WorkAssignment assignment, Location location) {
        if (location.getFullCode().compareTo(assignment.getStartLocation().getFullCode()) < 0) {
            throw new BusinessException(
                "LOCATION_BEFORE_START",
                "배정된 시작 로케이션보다 앞선 위치는 진행 위치로 기록할 수 없습니다."
            );
        }
    }

    private void changeMateStatus(Mate mate, MateStatus status, String whereabouts) {
        mate.changeStatus(status, whereabouts);
        mateStatusHistoryRepository.save(new MateStatusHistory(mate, status, whereabouts));
    }

    private String defaultWhereabouts(MateStatus status) {
        return switch (status) {
            case AVAILABLE -> "대기";
            case WORKING -> "업무중";
            case BREAK -> "휴게";
            case AWAY -> "자리비움";
            case OFF_DUTY -> "퇴근";
        };
    }

    private Mate mate(Long id) {
        return mateRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("MATE_NOT_FOUND", "MATE를 찾을 수 없습니다."));
    }

    private Location location(Long id) {
        return locationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("LOCATION_NOT_FOUND", "로케이션을 찾을 수 없습니다."));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
