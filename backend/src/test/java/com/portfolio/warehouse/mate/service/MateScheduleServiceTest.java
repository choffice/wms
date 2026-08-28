package com.portfolio.warehouse.mate.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.log.service.BusinessAuditService;
import com.portfolio.warehouse.mate.api.dto.ScheduleItemRequest;
import com.portfolio.warehouse.mate.domain.*;
import com.portfolio.warehouse.mate.repository.*;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MateScheduleServiceTest {

    @Test
    void overnightEndBeforeStartIsAccepted() {
        MateRepository mateRepository =
            mock(MateRepository.class);
        MateWorkScheduleRepository scheduleRepository =
            mock(MateWorkScheduleRepository.class);
        BusinessAuditService auditService =
            mock(BusinessAuditService.class);
        Mate mate = mock(Mate.class);

        when(mateRepository.findById(1L))
            .thenReturn(Optional.of(mate));
        when(mate.getNickname()).thenReturn("야간A");
        when(
            scheduleRepository
                .findAllByMateIdOrderByDayOfWeekAsc(1L)
        ).thenReturn(List.of());
        when(scheduleRepository.saveAll(anyList()))
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );

        MateScheduleService service =
            new MateScheduleService(
                mateRepository,
                scheduleRepository,
                auditService
            );

        var result =
            service.replace(
                1L,
                List.of(
                    new ScheduleItemRequest(
                        DayOfWeek.MONDAY,
                        ScheduleType.WEEKDAY,
                        ShiftType.CLOSING,
                        LocalTime.of(22, 0),
                        LocalTime.of(6, 0)
                    )
                )
            );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).startTime())
            .isEqualTo(LocalTime.of(22, 0));
        assertThat(result.get(0).endTime())
            .isEqualTo(LocalTime.of(6, 0));
    }

    @Test
    void equalStartAndEndIsRejected() {
        MateRepository mateRepository =
            mock(MateRepository.class);
        MateWorkScheduleRepository scheduleRepository =
            mock(MateWorkScheduleRepository.class);
        BusinessAuditService auditService =
            mock(BusinessAuditService.class);
        Mate mate = mock(Mate.class);

        when(mateRepository.findById(1L))
            .thenReturn(Optional.of(mate));

        MateScheduleService service =
            new MateScheduleService(
                mateRepository,
                scheduleRepository,
                auditService
            );

        assertThatThrownBy(() ->
            service.replace(
                1L,
                List.of(
                    new ScheduleItemRequest(
                        DayOfWeek.MONDAY,
                        ScheduleType.WEEKDAY,
                        ShiftType.DAY,
                        LocalTime.of(8, 0),
                        LocalTime.of(8, 0)
                    )
                )
            )
        ).hasMessageContaining(
            "같을 수 없습니다"
        );

        verify(scheduleRepository, never())
            .deleteAllByMateId(anyLong());
    }
}
