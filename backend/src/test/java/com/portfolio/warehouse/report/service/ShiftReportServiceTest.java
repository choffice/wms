package com.portfolio.warehouse.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.issue.repository.SpecialIssueRepository;
import com.portfolio.warehouse.location.domain.Location;
import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.mate.service.WorkScheduleResolver;
import com.portfolio.warehouse.pda.domain.*;
import com.portfolio.warehouse.pda.repository.PdaUsageHistoryRepository;
import com.portfolio.warehouse.report.api.dto.ShiftReportResponse;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.repository.*;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ShiftReportServiceTest {

    @Test
    void overnightSessionBelongsEntirelyToStartShiftDate() {
        WorkSessionQueryRepository sessionQueryRepository =
            mock(WorkSessionQueryRepository.class);
        PdaUsageHistoryRepository pdaUsageRepository =
            mock(PdaUsageHistoryRepository.class);
        SpecialIssueRepository issueRepository =
            mock(SpecialIssueRepository.class);
        WorkProgressRepository progressRepository =
            mock(WorkProgressRepository.class);
        WorkScheduleResolver scheduleResolver =
            mock(WorkScheduleResolver.class);

        WorkSession session = mock(WorkSession.class);
        WorkAssignment assignment = mock(WorkAssignment.class);
        WorkType workType = mock(WorkType.class);
        Mate mate = mock(Mate.class);
        PdaUsageHistory usage = mock(PdaUsageHistory.class);
        PdaDevice device = mock(PdaDevice.class);
        Location area = mock(Location.class);
        Location start = mock(Location.class);

        LocalDate shiftDate =
            LocalDate.of(2026, 8, 27);

        LocalDateTime startedAt =
            LocalDateTime.of(2026, 8, 27, 22, 0);

        LocalDateTime endedAt =
            LocalDateTime.of(2026, 8, 28, 6, 0);

        when(session.getShiftDate())
            .thenReturn(shiftDate);
        when(session.getStartedAt())
            .thenReturn(startedAt);
        when(session.getEndedAt())
            .thenReturn(endedAt);
        when(session.getQualityStatus())
            .thenReturn(WorkSessionQualityStatus.NORMAL);
        when(session.getWorkAssignment())
            .thenReturn(assignment);
        when(session.getMate()).thenReturn(mate);
        when(session.getPdaUsageHistory())
            .thenReturn(usage);

        when(assignment.getId()).thenReturn(17L);
        when(assignment.getWorkType()).thenReturn(workType);
        when(assignment.getAreaLocation()).thenReturn(area);
        when(assignment.getStartLocation()).thenReturn(start);

        when(workType.getName()).thenReturn("재고조사");
        when(mate.getId()).thenReturn(1L);
        when(mate.getNickname()).thenReturn("야간A");
        when(usage.getId()).thenReturn(9L);
        when(usage.getMate()).thenReturn(mate);
        when(usage.getPdaDevice()).thenReturn(device);
        when(usage.getAssignedAt())
            .thenReturn(
                LocalDateTime.of(
                    2026, 8, 27, 21, 50
                )
            );
        when(device.getDeviceNumber()).thenReturn(32);
        when(area.getFullCode()).thenReturn("A01");
        when(start.getFullCode()).thenReturn("A01-01");

        when(
            sessionQueryRepository.search(
                eq(shiftDate.atStartOfDay()),
                eq(
                    shiftDate
                        .plusDays(2)
                        .atStartOfDay()
                ),
                isNull(),
                isNull(),
                isNull()
            )
        ).thenReturn(List.of(session));

        when(
            sessionQueryRepository.search(
                eq(
                    shiftDate
                        .minusDays(14)
                        .atStartOfDay()
                ),
                eq(shiftDate.atStartOfDay()),
                isNull(),
                isNull(),
                isNull()
            )
        ).thenReturn(List.of());

        when(
            pdaUsageRepository.findOverlapping(
                any(LocalDateTime.class),
                any(LocalDateTime.class)
            )
        ).thenReturn(List.of(usage));

        when(
            issueRepository
                .findAllByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                    any(LocalDateTime.class),
                    any(LocalDateTime.class)
                )
        ).thenReturn(List.of());

        when(
            progressRepository
                .findFirstByWorkAssignmentIdAndMateIdAndReportedAtLessThanOrderByReportedAtDesc(
                    eq(17L),
                    eq(1L),
                    any(LocalDateTime.class)
                )
        ).thenReturn(Optional.empty());

        when(
            scheduleResolver.overnight(
                1L,
                shiftDate
            )
        ).thenReturn(true);

        ReportService service =
            new ReportService(
                sessionQueryRepository,
                pdaUsageRepository,
                issueRepository,
                progressRepository,
                scheduleResolver
            );

        ShiftReportResponse result =
            service.shift(shiftDate);

        assertThat(result.shiftDate())
            .isEqualTo(shiftDate);

        assertThat(
            result.summary().actualWorkSeconds()
        ).isEqualTo(8 * 60 * 60);

        assertThat(
            result.summary()
                .overnightSessionCount()
        ).isEqualTo(1);

        assertThat(result.works())
            .hasSize(1);

        assertThat(
            result.works()
                .get(0)
                .actualWorkSeconds()
        ).isEqualTo(8 * 60 * 60);
    }
}
