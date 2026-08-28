package com.portfolio.warehouse.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.issue.repository.SpecialIssueRepository;
import com.portfolio.warehouse.pda.repository.PdaUsageHistoryRepository;
import com.portfolio.warehouse.mate.service.WorkScheduleResolver;
import com.portfolio.warehouse.report.api.dto.WorkTimeStatResponse;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.repository.WorkSessionQueryRepository;
import com.portfolio.warehouse.work.repository.WorkProgressRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportServiceTest {

    @Test
    void workTimeIsClippedToRequestedDayBoundary() {
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

        when(session.getStartedAt()).thenReturn(
            LocalDateTime.of(2026, 8, 26, 23, 30)
        );
        when(session.getEndedAt()).thenReturn(
            LocalDateTime.of(2026, 8, 27, 1, 30)
        );
        when(session.getWorkAssignment()).thenReturn(assignment);
        when(assignment.getWorkType()).thenReturn(workType);
        when(workType.getName()).thenReturn("재고조사");

        when(
            sessionQueryRepository.search(
                any(),
                any(),
                isNull(),
                isNull(),
                eq(WorkSessionQualityStatus.NORMAL)
            )
        ).thenReturn(List.of(session));

        ReportService service = new ReportService(
            sessionQueryRepository,
            pdaUsageRepository,
            issueRepository,
            progressRepository,
            scheduleResolver
        );

        List<WorkTimeStatResponse> result =
            service.workTypeStats(
                LocalDate.of(2026, 8, 27),
                LocalDate.of(2026, 8, 27),
                null,
                null,
                false
            );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).totalSeconds())
            .isEqualTo(90 * 60);
        assertThat(result.get(0).averageSeconds())
            .isEqualTo(90 * 60);
    }
}
