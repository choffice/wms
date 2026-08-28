package com.portfolio.warehouse.work.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.common.event.OperationalEventService;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.mate.domain.*;
import com.portfolio.warehouse.mate.repository.MateStatusHistoryRepository;
import com.portfolio.warehouse.mate.service.WorkScheduleResolver;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.repository.WorkSessionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WorkSessionReliabilityServiceTest {

    @Test
    void longHeartbeatLossClosesSessionAsUncertainNetworkTimeout() {
        WorkSessionRepository sessionRepository =
            mock(WorkSessionRepository.class);
        WorkScheduleResolver scheduleResolver =
            mock(WorkScheduleResolver.class);
        MateStatusHistoryRepository statusHistoryRepository =
            mock(MateStatusHistoryRepository.class);
        OperationalEventService eventService =
            mock(OperationalEventService.class);

        WorkSession session = mock(WorkSession.class);
        Mate mate = mock(Mate.class);
        UserAccount account = mock(UserAccount.class);

        LocalDateTime now =
            LocalDateTime.of(2026, 8, 27, 12, 0);

        when(sessionRepository.findAllByEndedAtIsNull())
            .thenReturn(List.of(session));
        when(session.getLastHeartbeatAt())
            .thenReturn(now.minusMinutes(11));
        when(session.getMate()).thenReturn(mate);
        when(session.getShiftDate()).thenReturn(now.toLocalDate());
        when(session.getId()).thenReturn(10L);
        when(mate.getId()).thenReturn(1L);
        when(mate.getAccount()).thenReturn(account);
        when(scheduleResolver.extensionActive(1L, now.toLocalDate()))
            .thenReturn(false);
        when(scheduleResolver.effectiveEnd(1L, now.toLocalDate()))
            .thenReturn(Optional.empty());

        WorkSessionReliabilityService service =
            new WorkSessionReliabilityService(
                sessionRepository,
                scheduleResolver,
                statusHistoryRepository,
                3,
                10,
                eventService
            );

        service.inspectOpenSessions(now);

        verify(session).markUncertain();
        verify(session).close(
            now,
            WorkSessionEndReason.NETWORK_TIMEOUT
        );
        verify(mate).changeStatus(
            MateStatus.AWAY,
            "통신 확인 필요"
        );
        verify(eventService).publish(
            eq(ActivityType.SESSION_TIMEOUT),
            eq(account),
            eq("관리자"),
            contains("UNCERTAIN"),
            eq("WORK_SESSION"),
            eq(10L),
            eq(true),
            eq(true)
        );
    }

    @Test
    void scheduledEndTakesPriorityOverNetworkTimeout() {
        WorkSessionRepository sessionRepository =
            mock(WorkSessionRepository.class);
        WorkScheduleResolver scheduleResolver =
            mock(WorkScheduleResolver.class);
        MateStatusHistoryRepository statusHistoryRepository =
            mock(MateStatusHistoryRepository.class);
        OperationalEventService eventService =
            mock(OperationalEventService.class);

        WorkSession session = mock(WorkSession.class);
        Mate mate = mock(Mate.class);
        UserAccount account = mock(UserAccount.class);

        LocalDateTime now =
            LocalDateTime.of(2026, 8, 27, 18, 15);
        LocalDateTime scheduledEnd =
            LocalDateTime.of(2026, 8, 27, 18, 0);

        when(sessionRepository.findAllByEndedAtIsNull())
            .thenReturn(List.of(session));
        when(session.getLastHeartbeatAt())
            .thenReturn(now.minusMinutes(20));
        when(session.getMate()).thenReturn(mate);
        when(session.getShiftDate()).thenReturn(now.toLocalDate());
        when(session.getId()).thenReturn(11L);
        when(mate.getId()).thenReturn(1L);
        when(mate.getAccount()).thenReturn(account);
        when(scheduleResolver.extensionActive(1L, now.toLocalDate()))
            .thenReturn(false);
        when(scheduleResolver.effectiveEnd(1L, now.toLocalDate()))
            .thenReturn(Optional.of(scheduledEnd));

        WorkSessionReliabilityService service =
            new WorkSessionReliabilityService(
                sessionRepository,
                scheduleResolver,
                statusHistoryRepository,
                3,
                10,
                eventService
            );

        service.inspectOpenSessions(now);

        verify(session).markUncertain();
        verify(session).close(
            scheduledEnd,
            WorkSessionEndReason.SCHEDULE_END
        );
        verify(session, never()).close(
            any(),
            eq(WorkSessionEndReason.NETWORK_TIMEOUT)
        );
        verify(mate).changeStatus(
            MateStatus.OFF_DUTY,
            "퇴근"
        );
        verify(eventService).publish(
            eq(ActivityType.SHIFT_AUTO_END),
            eq(account),
            eq("관리자"),
            contains("자동 종료"),
            eq("WORK_SESSION"),
            eq(11L),
            eq(true),
            eq(true)
        );
    }

    @Test
    void overnightShiftUsesStartDateForScheduledEnd() {
        WorkSessionRepository sessionRepository =
            mock(WorkSessionRepository.class);
        WorkScheduleResolver scheduleResolver =
            mock(WorkScheduleResolver.class);
        MateStatusHistoryRepository statusHistoryRepository =
            mock(MateStatusHistoryRepository.class);
        OperationalEventService eventService =
            mock(OperationalEventService.class);

        WorkSession session = mock(WorkSession.class);
        Mate mate = mock(Mate.class);
        UserAccount account = mock(UserAccount.class);

        LocalDate shiftDate =
            LocalDate.of(2026, 8, 27);

        LocalDateTime now =
            LocalDateTime.of(2026, 8, 28, 6, 15);

        LocalDateTime scheduledEnd =
            LocalDateTime.of(2026, 8, 28, 6, 0);

        when(sessionRepository.findAllByEndedAtIsNull())
            .thenReturn(List.of(session));
        when(session.getLastHeartbeatAt())
            .thenReturn(now.minusMinutes(20));
        when(session.getMate()).thenReturn(mate);
        when(session.getShiftDate()).thenReturn(shiftDate);
        when(session.getId()).thenReturn(21L);
        when(mate.getId()).thenReturn(1L);
        when(mate.getAccount()).thenReturn(account);

        when(
            scheduleResolver.extensionActive(
                1L,
                shiftDate
            )
        ).thenReturn(false);

        when(
            scheduleResolver.effectiveEnd(
                1L,
                shiftDate
            )
        ).thenReturn(
            Optional.of(scheduledEnd)
        );

        WorkSessionReliabilityService service =
            new WorkSessionReliabilityService(
                sessionRepository,
                scheduleResolver,
                statusHistoryRepository,
                3,
                10,
                eventService
            );

        service.inspectOpenSessions(now);

        verify(session).close(
            scheduledEnd,
            WorkSessionEndReason.SCHEDULE_END
        );

        verify(scheduleResolver).effectiveEnd(
            1L,
            shiftDate
        );

        verify(scheduleResolver, never())
            .effectiveEnd(
                1L,
                now.toLocalDate()
            );
    }


    @Test
    void scheduledEndBeforeSessionStartDoesNotCreateNegativeDuration() {
        WorkSessionRepository sessionRepository =
            mock(WorkSessionRepository.class);
        WorkScheduleResolver scheduleResolver =
            mock(WorkScheduleResolver.class);
        MateStatusHistoryRepository statusHistoryRepository =
            mock(MateStatusHistoryRepository.class);
        OperationalEventService eventService =
            mock(OperationalEventService.class);

        WorkSession session = mock(WorkSession.class);
        Mate mate = mock(Mate.class);

        LocalDate shiftDate =
            LocalDate.of(2026, 8, 27);

        LocalDateTime now =
            LocalDateTime.of(2026, 8, 27, 19, 10);

        when(sessionRepository.findAllByEndedAtIsNull())
            .thenReturn(List.of(session));
        when(session.getMate()).thenReturn(mate);
        when(session.getShiftDate()).thenReturn(shiftDate);
        when(session.getStartedAt())
            .thenReturn(
                LocalDateTime.of(
                    2026, 8, 27, 19, 0
                )
            );
        when(session.getLastHeartbeatAt())
            .thenReturn(now.minusMinutes(1));
        when(mate.getId()).thenReturn(1L);

        when(
            scheduleResolver.extensionActive(
                1L,
                shiftDate
            )
        ).thenReturn(false);

        when(
            scheduleResolver.effectiveEnd(
                1L,
                shiftDate
            )
        ).thenReturn(
            Optional.of(
                LocalDateTime.of(
                    2026, 8, 27, 18, 0
                )
            )
        );

        WorkSessionReliabilityService service =
            new WorkSessionReliabilityService(
                sessionRepository,
                scheduleResolver,
                statusHistoryRepository,
                3,
                10,
                eventService
            );

        service.inspectOpenSessions(now);

        verify(session, never()).close(
            any(),
            eq(WorkSessionEndReason.SCHEDULE_END)
        );
        verify(session, never()).close(
            any(),
            eq(WorkSessionEndReason.NETWORK_TIMEOUT)
        );
    }

}
