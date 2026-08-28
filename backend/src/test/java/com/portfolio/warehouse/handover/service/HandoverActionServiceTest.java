package com.portfolio.warehouse.handover.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.common.event.OperationalEventService;
import com.portfolio.warehouse.handover.api.dto.*;
import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.mate.repository.MateRepository;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.repository.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HandoverActionServiceTest {

    @Test
    void staleRowAbortsBatchBeforeAnyAssignmentIsChanged() {
        WorkAssignmentRepository assignmentRepository =
            mock(WorkAssignmentRepository.class);
        WorkSessionRepository sessionRepository =
            mock(WorkSessionRepository.class);
        WorkAssignmentHistoryRepository historyRepository =
            mock(WorkAssignmentHistoryRepository.class);
        MateRepository mateRepository =
            mock(MateRepository.class);
        CurrentUserService currentUserService =
            mock(CurrentUserService.class);
        OperationalEventService eventService =
            mock(OperationalEventService.class);

        WorkAssignment first =
            mock(WorkAssignment.class);
        WorkAssignment stale =
            mock(WorkAssignment.class);

        Mate mate1 = mock(Mate.class);
        Mate mate2 = mock(Mate.class);
        Mate mate3 = mock(Mate.class);
        Mate mate4 = mock(Mate.class);
        Mate actualStaleMate = mock(Mate.class);

        when(assignmentRepository.findByIdForUpdate(10L))
            .thenReturn(Optional.of(first));
        when(assignmentRepository.findByIdForUpdate(20L))
            .thenReturn(Optional.of(stale));

        when(first.getId()).thenReturn(10L);
        when(first.getStatus())
            .thenReturn(WorkAssignmentStatus.IN_PROGRESS);
        when(first.getCurrentMate()).thenReturn(mate1);
        when(mate1.getId()).thenReturn(1L);

        when(stale.getId()).thenReturn(20L);
        when(stale.getStatus())
            .thenReturn(WorkAssignmentStatus.IN_PROGRESS);
        when(stale.getCurrentMate())
            .thenReturn(actualStaleMate);
        when(actualStaleMate.getId()).thenReturn(99L);

        when(mateRepository.findByIdForUpdate(1L))
            .thenReturn(Optional.of(mate1));
        when(mateRepository.findByIdForUpdate(2L))
            .thenReturn(Optional.of(mate2));
        when(mateRepository.findByIdForUpdate(3L))
            .thenReturn(Optional.of(mate3));
        when(mateRepository.findByIdForUpdate(4L))
            .thenReturn(Optional.of(mate4));

        when(mate3.isActive()).thenReturn(true);
        when(mate3.getNickname()).thenReturn("새담당A");
        when(mate4.isActive()).thenReturn(true);
        when(mate4.getNickname()).thenReturn("새담당B");

        when(
            sessionRepository
                .findFirstByWorkAssignmentIdAndEndedAtIsNull(10L)
        ).thenReturn(Optional.empty());

        HandoverActionService service =
            new HandoverActionService(
                assignmentRepository,
                sessionRepository,
                historyRepository,
                mateRepository,
                currentUserService,
                eventService
            );

        BulkHandoverRequest request =
            new BulkHandoverRequest(
                List.of(
                    new BulkHandoverItemRequest(
                        10L,
                        1L,
                        3L,
                        "교대"
                    ),
                    new BulkHandoverItemRequest(
                        20L,
                        2L,
                        4L,
                        "교대"
                    )
                )
            );

        assertThatThrownBy(() ->
            service.bulkTransfer(request)
        ).hasMessageContaining(
            "담당자가 다른 요청으로 변경"
        );

        verify(first, never()).tradeTo(any());
        verify(stale, never()).tradeTo(any());
        verify(historyRepository, never()).save(any());
        verify(eventService, never()).publish(
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
