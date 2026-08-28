package com.portfolio.warehouse.mate.service;

import com.portfolio.warehouse.mate.domain.*;
import com.portfolio.warehouse.mate.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkScheduleResolver {

    private final MateWorkScheduleRepository scheduleRepository;
    private final WorkScheduleOverrideRepository overrideRepository;

    public WorkScheduleResolver(
        MateWorkScheduleRepository scheduleRepository,
        WorkScheduleOverrideRepository overrideRepository
    ) {
        this.scheduleRepository = scheduleRepository;
        this.overrideRepository = overrideRepository;
    }

    @Transactional(readOnly = true)
    public boolean extensionActive(
        Long mateId,
        LocalDate shiftDate
    ) {
        return overrideRepository
            .existsByMateIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndOverrideTypeAndAutoEndDisabledTrue(
                mateId,
                shiftDate,
                shiftDate,
                ScheduleOverrideType.EXTENSION
            );
    }

    @Transactional(readOnly = true)
    public Optional<ResolvedWorkShift> resolveForShiftDate(
        Long mateId,
        LocalDate shiftDate
    ) {
        List<WorkScheduleOverride> overrides =
            overrideRepository
                .findAllByMateIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    mateId,
                    shiftDate,
                    shiftDate
                );

        Optional<WorkScheduleOverride> period =
            overrides.stream()
                .filter(override ->
                    override.getOverrideType()
                        == ScheduleOverrideType.PERIOD_SCHEDULE
                )
                .filter(override ->
                    override.getStartTime() != null
                        && override.getEndTime() != null
                )
                .max(
                    Comparator.comparing(
                        (WorkScheduleOverride item) ->
                            item.getId()
                    )
                );

        LocalTime startTime;
        LocalTime endTime;
        String source;

        if (period.isPresent()) {
            startTime = period.get().getStartTime();
            endTime = period.get().getEndTime();
            source = "PERIOD_OVERRIDE";
        } else {
            Optional<MateWorkSchedule> schedule =
                scheduleRepository
                    .findAllByMateIdOrderByDayOfWeekAsc(
                        mateId
                    )
                    .stream()
                    .filter(item ->
                        item.getDayOfWeek()
                            == shiftDate.getDayOfWeek()
                    )
                    .findFirst();

            if (schedule.isEmpty()) {
                return Optional.empty();
            }

            startTime = schedule.get().getStartTime();
            endTime = schedule.get().getEndTime();
            source = "BASE_SCHEDULE";
        }

        if (startTime.equals(endTime)) {
            return Optional.empty();
        }

        boolean overnight =
            endTime.isBefore(startTime);

        LocalDateTime startsAt =
            shiftDate.atTime(startTime);

        LocalDateTime endsAt =
            (
                overnight
                    ? shiftDate.plusDays(1)
                    : shiftDate
            ).atTime(endTime);

        boolean extension =
            extensionActive(
                mateId,
                shiftDate
            );

        return Optional.of(
            new ResolvedWorkShift(
                shiftDate,
                startsAt,
                endsAt,
                overnight,
                extension,
                !extension,
                source
            )
        );
    }

    @Transactional(readOnly = true)
    public LocalDate resolveShiftDate(
        Long mateId,
        LocalDateTime moment
    ) {
        LocalDate today =
            moment.toLocalDate();

        List<ResolvedWorkShift> normalCandidates =
            new ArrayList<>();

        resolveForShiftDate(
            mateId,
            today.minusDays(1)
        ).filter(shift ->
            containsNominally(shift, moment)
        ).ifPresent(normalCandidates::add);

        resolveForShiftDate(
            mateId,
            today
        ).filter(shift ->
            containsNominally(shift, moment)
        ).ifPresent(normalCandidates::add);

        if (!normalCandidates.isEmpty()) {
            return normalCandidates.stream()
                .max(
                    Comparator.comparing(
                        (ResolvedWorkShift item) ->
                            item.startsAt()
                    )
                )
                .orElseThrow()
                .shiftDate();
        }

        // Extension은 nominal end 이후에도 수동 종료될 수 있다.
        // 전날 shift가 연장 상태이고 다음 shift가 아직 시작하지 않았다면
        // 계속 전날 shiftDate에 귀속한다.
        Optional<ResolvedWorkShift> previous =
            resolveForShiftDate(
                mateId,
                today.minusDays(1)
            );

        if (
            previous.isPresent()
                && previous.get().extensionActive()
                && !moment.isBefore(
                    previous.get().startsAt()
                )
                && beforeNextShiftStart(
                    mateId,
                    previous.get().shiftDate(),
                    moment
                )
        ) {
            return previous.get().shiftDate();
        }

        Optional<ResolvedWorkShift> current =
            resolveForShiftDate(
                mateId,
                today
            );

        if (
            current.isPresent()
                && current.get().extensionActive()
                && !moment.isBefore(
                    current.get().startsAt()
                )
        ) {
            return current.get().shiftDate();
        }

        // 스케줄이 없거나 실제 작업이 스케줄 밖에서 시작된 경우에는
        // 데이터 손실보다 시작 날짜 귀속을 우선한다.
        return today;
    }

    @Transactional(readOnly = true)
    public Optional<LocalDateTime> effectiveEnd(
        Long mateId,
        LocalDate shiftDate
    ) {
        return resolveForShiftDate(
            mateId,
            shiftDate
        ).map(ResolvedWorkShift::endsAt);
    }

    @Transactional(readOnly = true)
    public Optional<LocalDateTime> effectiveStart(
        Long mateId,
        LocalDate shiftDate
    ) {
        return resolveForShiftDate(
            mateId,
            shiftDate
        ).map(ResolvedWorkShift::startsAt);
    }

    @Transactional(readOnly = true)
    public boolean overnight(
        Long mateId,
        LocalDate shiftDate
    ) {
        return resolveForShiftDate(
            mateId,
            shiftDate
        )
            .map(ResolvedWorkShift::overnight)
            .orElse(false);
    }

    private boolean containsNominally(
        ResolvedWorkShift shift,
        LocalDateTime moment
    ) {
        return !moment.isBefore(shift.startsAt())
            && moment.isBefore(shift.endsAt());
    }

    private boolean beforeNextShiftStart(
        Long mateId,
        LocalDate shiftDate,
        LocalDateTime moment
    ) {
        Optional<ResolvedWorkShift> next =
            resolveForShiftDate(
                mateId,
                shiftDate.plusDays(1)
            );

        if (next.isPresent()) {
            return moment.isBefore(
                next.get().startsAt()
            );
        }

        // 다음 근무일이 비어 있는 경우에도 무제한으로 전날 shift에
        // 귀속시키지 않도록 최대 36시간까지만 extension shift로 본다.
        return moment.isBefore(
            shiftDate
                .plusDays(1)
                .atStartOfDay()
                .plusHours(12)
        );
    }
}
