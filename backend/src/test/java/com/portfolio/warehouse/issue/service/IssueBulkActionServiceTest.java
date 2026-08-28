package com.portfolio.warehouse.issue.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.common.event.OperationalEventService;
import com.portfolio.warehouse.issue.api.dto.*;
import com.portfolio.warehouse.issue.domain.*;
import com.portfolio.warehouse.issue.repository.*;
import com.portfolio.warehouse.mate.repository.MateRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IssueBulkActionServiceTest {

    @Test
    void staleStatusAbortsBulkConfirmBeforeAnyIssueChanges() {
        SpecialIssueRepository issueRepository =
            mock(SpecialIssueRepository.class);
        SpecialIssueHistoryRepository historyRepository =
            mock(SpecialIssueHistoryRepository.class);
        MateRepository mateRepository =
            mock(MateRepository.class);
        CurrentUserService currentUserService =
            mock(CurrentUserService.class);
        OperationalEventService eventService =
            mock(OperationalEventService.class);

        SpecialIssue first =
            mock(SpecialIssue.class);
        SpecialIssue stale =
            mock(SpecialIssue.class);

        when(issueRepository.findByIdForUpdate(10L))
            .thenReturn(Optional.of(first));
        when(issueRepository.findByIdForUpdate(20L))
            .thenReturn(Optional.of(stale));

        when(first.getId()).thenReturn(10L);
        when(first.getDeletedAt()).thenReturn(null);
        when(first.getStatus())
            .thenReturn(IssueStatus.UNCONFIRMED);

        when(stale.getId()).thenReturn(20L);
        when(stale.getDeletedAt()).thenReturn(null);
        when(stale.getStatus())
            .thenReturn(IssueStatus.CONFIRMED);

        IssueBulkActionService service =
            new IssueBulkActionService(
                issueRepository,
                historyRepository,
                mateRepository,
                currentUserService,
                eventService
            );

        BulkIssueStatusRequest request =
            new BulkIssueStatusRequest(
                List.of(
                    new BulkIssueStatusItemRequest(
                        10L,
                        "UNCONFIRMED"
                    ),
                    new BulkIssueStatusItemRequest(
                        20L,
                        "UNCONFIRMED"
                    )
                )
            );

        assertThatThrownBy(() ->
            service.bulkConfirm(request)
        ).hasMessageContaining(
            "다른 요청으로 변경"
        );

        verify(first, never()).confirm();
        verify(stale, never()).confirm();
        verify(historyRepository, never())
            .save(any());
        verify(eventService, never())
            .publish(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                anyBoolean(),
                anyBoolean()
            );
    }
}
