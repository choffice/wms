package com.portfolio.warehouse.mate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.mate.domain.*;
import com.portfolio.warehouse.mate.repository.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkScheduleResolverTest {

    @Test
    void overnightShiftUsesStartDateAndEndsNextDay() {
        MateWorkScheduleRepository scheduleRepository =
            mock(MateWorkScheduleRepository.class);
        WorkScheduleOverrideRepository overrideRepository =
            mock(WorkScheduleOverrideRepository.class);

        MateWorkSchedule schedule =
            mock(MateWorkSchedule.class);

        when(schedule.getDayOfWeek())
            .thenReturn(DayOfWeek.MONDAY);
        when(schedule.getStartTime())
            .thenReturn(LocalTime.of(22, 0));
        when(schedule.getEndTime())
            .thenReturn(LocalTime.of(6, 0));

        when(
            scheduleRepository
                .findAllByMateIdOrderByDayOfWeekAsc(1L)
        ).thenReturn(List.of(schedule));

        when(
            overrideRepository
                .findAllByMateIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    eq(1L),
                    any(LocalDate.class),
                    any(LocalDate.class)
                )
        ).thenReturn(List.of());

        when(
            overrideRepository
                .existsByMateIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndOverrideTypeAndAutoEndDisabledTrue(
                    eq(1L),
                    any(LocalDate.class),
                    any(LocalDate.class),
                    eq(ScheduleOverrideType.EXTENSION)
                )
        ).thenReturn(false);

        WorkScheduleResolver resolver =
            new WorkScheduleResolver(
                scheduleRepository,
                overrideRepository
            );

        LocalDate monday =
            LocalDate.of(2026, 8, 24);

        ResolvedWorkShift resolved =
            resolver
                .resolveForShiftDate(
                    1L,
                    monday
                )
                .orElseThrow();

        assertThat(resolved.shiftDate())
            .isEqualTo(monday);
        assertThat(resolved.startsAt())
            .isEqualTo(
                LocalDateTime.of(
                    2026, 8, 24, 22, 0
                )
            );
        assertThat(resolved.endsAt())
            .isEqualTo(
                LocalDateTime.of(
                    2026, 8, 25, 6, 0
                )
            );
        assertThat(resolved.overnight())
            .isTrue();

        assertThat(
            resolver.resolveShiftDate(
                1L,
                LocalDateTime.of(
                    2026, 8, 25, 2, 0
                )
            )
        ).isEqualTo(monday);
    }
}
