package com.portfolio.warehouse.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.issue.repository.SpecialIssueRepository;
import com.portfolio.warehouse.mate.service.WorkScheduleResolver;
import com.portfolio.warehouse.pda.repository.PdaUsageHistoryRepository;
import com.portfolio.warehouse.work.domain.WorkSession;
import com.portfolio.warehouse.work.repository.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecentShiftDatesTest {

    @Test
    void recentShiftDatesAreDistinctAndNewestFirst() {
        WorkSessionQueryRepository query =
            mock(WorkSessionQueryRepository.class);
        PdaUsageHistoryRepository pda =
            mock(PdaUsageHistoryRepository.class);
        SpecialIssueRepository issues =
            mock(SpecialIssueRepository.class);
        WorkProgressRepository progress =
            mock(WorkProgressRepository.class);
        WorkScheduleResolver resolver =
            mock(WorkScheduleResolver.class);

        WorkSession first = mock(WorkSession.class);
        WorkSession duplicate = mock(WorkSession.class);
        WorkSession newer = mock(WorkSession.class);

        when(first.getShiftDate())
            .thenReturn(
                LocalDate.of(2026, 8, 26)
            );
        when(duplicate.getShiftDate())
            .thenReturn(
                LocalDate.of(2026, 8, 26)
            );
        when(newer.getShiftDate())
            .thenReturn(
                LocalDate.of(2026, 8, 27)
            );

        when(
            query.search(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                isNull(),
                isNull(),
                isNull()
            )
        ).thenReturn(
            List.of(first, duplicate, newer)
        );

        ReportService service =
            new ReportService(
                query,
                pda,
                issues,
                progress,
                resolver
            );

        assertThat(
            service.recentShiftDates(7)
        ).containsExactly(
            LocalDate.of(2026, 8, 27),
            LocalDate.of(2026, 8, 26)
        );
    }
}
