package com.portfolio.warehouse.mate.service;

import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.mate.domain.*;
import com.portfolio.warehouse.mate.repository.*;
import com.portfolio.warehouse.work.domain.WorkSession;
import com.portfolio.warehouse.work.domain.WorkSessionEndReason;
import com.portfolio.warehouse.work.repository.WorkSessionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftService {

    private final CurrentUserService currentUserService;
    private final WorkSessionRepository sessionRepository;
    private final MateStatusHistoryRepository statusHistoryRepository;
    private final WorkScheduleOverrideRepository overrideRepository;
    private final WorkScheduleResolver scheduleResolver;

    public ShiftService(
        CurrentUserService currentUserService,
        WorkSessionRepository sessionRepository,
        MateStatusHistoryRepository statusHistoryRepository,
        WorkScheduleOverrideRepository overrideRepository,
        WorkScheduleResolver scheduleResolver
    ) {
        this.currentUserService = currentUserService;
        this.sessionRepository = sessionRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.overrideRepository = overrideRepository;
        this.scheduleResolver = scheduleResolver;
    }

    @Transactional(readOnly = true)
    public com.portfolio.warehouse.mate.api.dto.TodayShiftResponse today() {
        Mate mate = currentUserService.mate();
        LocalDateTime now = LocalDateTime.now();

        WorkSession openSession =
            sessionRepository
                .findFirstByMateIdAndEndedAtIsNull(
                    mate.getId()
                )
                .orElse(null);

        LocalDate shiftDate =
            resolveShiftDate(
                mate.getId(),
                now,
                openSession
            );

        ResolvedWorkShift resolved =
            scheduleResolver
                .resolveForShiftDate(
                    mate.getId(),
                    shiftDate
                )
                .orElse(null);

        boolean extension =
            scheduleResolver.extensionActive(
                mate.getId(),
                shiftDate
            );

        return new com.portfolio.warehouse.mate.api.dto.TodayShiftResponse(
            now.toLocalDate(),
            shiftDate,
            mate.getCurrentStatus().name(),
            mate.getCurrentWhereabouts(),
            resolved == null
                ? null
                : resolved.startsAt(),
            resolved == null
                ? null
                : resolved.endsAt(),
            resolved != null
                && resolved.overnight(),
            extension,
            !extension && resolved != null
        );
    }

    @Transactional
    public void endShift() {
        Mate mate = currentUserService.mate();
        LocalDateTime now = LocalDateTime.now();

        WorkSession openSession =
            sessionRepository
                .findFirstByMateIdAndEndedAtIsNull(
                    mate.getId()
                )
                .orElse(null);

        LocalDate shiftDate =
            resolveShiftDate(
                mate.getId(),
                now,
                openSession
            );

        if (openSession != null) {
            openSession.close(
                now,
                WorkSessionEndReason.MANUAL_SHIFT_END
            );
        }

        overrideRepository
            .findAllByMateIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndOverrideType(
                mate.getId(),
                shiftDate,
                shiftDate,
                ScheduleOverrideType.EXTENSION
            )
            .forEach(overrideRepository::delete);

        mate.changeStatus(
            MateStatus.OFF_DUTY,
            "퇴근"
        );

        statusHistoryRepository.save(
            new MateStatusHistory(
                mate,
                MateStatus.OFF_DUTY,
                "퇴근"
            )
        );
    }

    private LocalDate resolveShiftDate(
        Long mateId,
        LocalDateTime now,
        WorkSession openSession
    ) {
        if (
            openSession != null
                && openSession.getShiftDate() != null
        ) {
            return openSession.getShiftDate();
        }

        if (openSession != null) {
            return scheduleResolver.resolveShiftDate(
                mateId,
                openSession.getStartedAt()
            );
        }

        return scheduleResolver.resolveShiftDate(
            mateId,
            now
        );
    }
}
