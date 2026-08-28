package com.portfolio.warehouse.mate.service;

import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.log.service.BusinessAuditService;
import com.portfolio.warehouse.mate.api.dto.*;
import com.portfolio.warehouse.mate.domain.*;
import com.portfolio.warehouse.mate.repository.*;
import com.portfolio.warehouse.work.repository.WorkSessionRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleOverrideService {

    private final WorkScheduleOverrideRepository repository;
    private final MateRepository mateRepository;
    private final CurrentUserService currentUserService;
    private final BusinessAuditService auditService;
    private final WorkScheduleResolver scheduleResolver;
    private final WorkSessionRepository sessionRepository;

    public ScheduleOverrideService(
        WorkScheduleOverrideRepository repository,
        MateRepository mateRepository,
        CurrentUserService currentUserService,
        BusinessAuditService auditService,
        WorkScheduleResolver scheduleResolver,
        WorkSessionRepository sessionRepository
    ) {
        this.repository = repository;
        this.mateRepository = mateRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.scheduleResolver = scheduleResolver;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public ScheduleOverrideResponse createPeriod(
        Long mateId,
        ScheduleOverrideRequest request
    ) {
        Mate mate = mateRepository.findById(mateId)
            .orElseThrow(() -> new NotFoundException(
                "MATE_NOT_FOUND",
                "MATE를 찾을 수 없습니다."
            ));

        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException(
                "INVALID_DATE_RANGE",
                "종료일은 시작일보다 빠를 수 없습니다."
            );
        }

        if (request.endTime().equals(request.startTime())) {
            throw new BusinessException(
                "INVALID_WORK_TIME",
                "근무 시작시간과 종료시간은 같을 수 없습니다."
            );
        }

        WorkScheduleOverride saved =
            repository.save(
                new WorkScheduleOverride(
                    mate,
                    request.startDate(),
                    request.endDate(),
                    request.startTime(),
                    request.endTime(),
                    ScheduleOverrideType.PERIOD_SCHEDULE,
                    false
                )
            );

        auditService.record(
            ActivityType.SCHEDULE_OVERRIDE_CHANGE,
            mate.getNickname(),
            "기간별 예외 근무시간 등록 · "
                + saved.getStartDate()
                + "~"
                + saved.getEndDate()
                + " / "
                + saved.getStartTime()
                + "~"
                + saved.getEndTime()
                + (
                    saved.getEndTime()
                        .isBefore(
                            saved.getStartTime()
                        )
                        ? "(익일)"
                        : ""
                ),
            "MATE_SCHEDULE",
            mate.getId()
        );

        return ScheduleOverrideResponse.from(saved);
    }

    @Transactional
    public ScheduleOverrideResponse extendToday() {
        return createExtension(
            currentUserService.mate()
        );
    }

    @Transactional
    public ScheduleOverrideResponse extendTodayForAdmin(
        Long mateId
    ) {
        Mate mate = mateRepository.findById(mateId)
            .orElseThrow(() -> new NotFoundException(
                "MATE_NOT_FOUND",
                "MATE를 찾을 수 없습니다."
            ));

        return createExtension(mate);
    }

    @Transactional
    public void cancelExtensionTodayForAdmin(
        Long mateId
    ) {
        Mate mate = mateRepository.findById(mateId)
            .orElseThrow(() -> new NotFoundException(
                "MATE_NOT_FOUND",
                "MATE를 찾을 수 없습니다."
            ));

        cancelExtension(mate);
    }

    @Transactional
    public void cancelExtensionToday() {
        cancelExtension(
            currentUserService.mate()
        );
    }

    private void cancelExtension(Mate mate) {
        LocalDate today = operationShiftDate(mate);

        List<WorkScheduleOverride> existing =
            repository
                .findAllByMateIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndOverrideType(
                    mate.getId(),
                    today,
                    today,
                    ScheduleOverrideType.EXTENSION
                );

        if (existing.isEmpty()) {
            return;
        }

        existing.forEach(repository::delete);

        auditService.record(
            ActivityType.EXTENSION_CHANGE,
            mate.getNickname(),
            "근무 기준일 "
                + today
                + " 연장 해제",
            "MATE_SCHEDULE",
            mate.getId()
        );
    }

    private LocalDate operationShiftDate(
        Mate mate
    ) {
        return sessionRepository
            .findFirstByMateIdAndEndedAtIsNull(
                mate.getId()
            )
            .map(session ->
                session.getShiftDate() != null
                    ? session.getShiftDate()
                    : scheduleResolver.resolveShiftDate(
                        mate.getId(),
                        session.getStartedAt()
                    )
            )
            .orElseGet(() ->
                scheduleResolver.resolveShiftDate(
                    mate.getId(),
                    java.time.LocalDateTime.now()
                )
            );
    }

    private ScheduleOverrideResponse createExtension(
        Mate mate
    ) {
        LocalDate today = operationShiftDate(mate);

        List<WorkScheduleOverride> existing =
            repository
                .findAllByMateIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndOverrideType(
                    mate.getId(),
                    today,
                    today,
                    ScheduleOverrideType.EXTENSION
                );

        if (!existing.isEmpty()) {
            return ScheduleOverrideResponse.from(
                existing.get(existing.size() - 1)
            );
        }

        WorkScheduleOverride saved =
            repository.save(
                new WorkScheduleOverride(
                    mate,
                    today,
                    today,
                    null,
                    null,
                    ScheduleOverrideType.EXTENSION,
                    true
                )
            );

        auditService.record(
            ActivityType.EXTENSION_CHANGE,
            mate.getNickname(),
            "근무 기준일 "
                + today
                + " 연장 활성",
            "MATE_SCHEDULE",
            mate.getId()
        );

        return ScheduleOverrideResponse.from(saved);
    }
}
