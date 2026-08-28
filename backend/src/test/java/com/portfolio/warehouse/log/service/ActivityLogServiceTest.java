package com.portfolio.warehouse.log.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.log.api.dto.ActivityLogPageResponse;
import com.portfolio.warehouse.log.domain.*;
import com.portfolio.warehouse.log.repository.*;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ActivityLogServiceTest {

    @Test
    void auditSearchIsPagedAndDatabaseHistoryIsNotDeleted() {
        ActivityLogRepository repository =
            mock(ActivityLogRepository.class);
        ActivityLogQueryRepository queryRepository =
            mock(ActivityLogQueryRepository.class);

        ActivityLog log = mock(ActivityLog.class);

        when(queryRepository.search(
            any(),
            any(),
            eq(ActivityType.WORK_ASSIGN),
            eq("AD0001"),
            eq("WORK_ASSIGNMENT"),
            isNull(),
            eq("A01"),
            eq(0),
            eq(50)
        )).thenReturn(List.of(log));

        when(queryRepository.count(
            any(),
            any(),
            eq(ActivityType.WORK_ASSIGN),
            eq("AD0001"),
            eq("WORK_ASSIGNMENT"),
            isNull(),
            eq("A01")
        )).thenReturn(1L);

        when(log.getId()).thenReturn(1L);
        when(log.getType())
            .thenReturn(ActivityType.WORK_ASSIGN);
        when(log.getMessage()).thenReturn("A01 업무 배정");

        ActivityLogService service =
            new ActivityLogService(
                repository,
                queryRepository
            );

        ActivityLogPageResponse result =
            service.search(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                ActivityType.WORK_ASSIGN,
                "AD0001",
                "WORK_ASSIGNMENT",
                null,
                "A01",
                0,
                50
            );

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(50);
        assertThat(result.totalPages()).isEqualTo(1);

        verify(repository, never())
            .deleteAllByIdInBatch(any());
    }
}
