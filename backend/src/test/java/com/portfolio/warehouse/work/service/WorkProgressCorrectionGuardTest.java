package com.portfolio.warehouse.work.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.common.event.OperationalEventService;
import com.portfolio.warehouse.location.domain.Location;
import com.portfolio.warehouse.location.repository.LocationRepository;
import com.portfolio.warehouse.mate.repository.*;
import com.portfolio.warehouse.mate.service.WorkScheduleResolver;
import com.portfolio.warehouse.pda.service.PdaSessionService;
import com.portfolio.warehouse.work.api.dto.AdminWorkProgressCorrectionRequest;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WorkProgressCorrectionGuardTest {

    @Test
    void staleAdminCorrectionIsRejectedBeforeOverwritingCurrentProgress() {
        WorkAssignmentRepository assignmentRepository =
            mock(WorkAssignmentRepository.class);
        WorkTypeRepository workTypeRepository =
            mock(WorkTypeRepository.class);
        WorkProgressRepository progressRepository =
            mock(WorkProgressRepository.class);
        WorkSessionRepository sessionRepository =
            mock(WorkSessionRepository.class);
        WorkAssignmentHistoryRepository historyRepository =
            mock(WorkAssignmentHistoryRepository.class);
        LocationRepository locationRepository =
            mock(LocationRepository.class);
        MateRepository mateRepository =
            mock(MateRepository.class);
        MateStatusHistoryRepository statusHistoryRepository =
            mock(MateStatusHistoryRepository.class);
        CurrentUserService currentUserService =
            mock(CurrentUserService.class);
        PdaSessionService pdaSessionService =
            mock(PdaSessionService.class);
        OperationalEventService eventService =
            mock(OperationalEventService.class);
        WorkScheduleResolver scheduleResolver =
            mock(WorkScheduleResolver.class);

        WorkAssignment assignment =
            mock(WorkAssignment.class);
        Location current = mock(Location.class);

        when(assignmentRepository.findByIdForUpdate(10L))
            .thenReturn(Optional.of(assignment));
        when(assignment.getStatus())
            .thenReturn(WorkAssignmentStatus.IN_PROGRESS);
        when(
            sessionRepository
                .findFirstByWorkAssignmentIdAndEndedAtIsNull(10L)
        ).thenReturn(Optional.empty());
        when(assignment.getCurrentLastCompletedLocation())
            .thenReturn(current);
        when(current.getId()).thenReturn(88L);

        WorkAssignmentService service =
            new WorkAssignmentService(
                assignmentRepository,
                workTypeRepository,
                progressRepository,
                sessionRepository,
                historyRepository,
                locationRepository,
                mateRepository,
                statusHistoryRepository,
                currentUserService,
                pdaSessionService,
                eventService,
                scheduleResolver
            );

        assertThatThrownBy(() ->
            service.adminCorrectProgress(
                10L,
                new AdminWorkProgressCorrectionRequest(
                    77L,
                    90L,
                    "현장 확인"
                )
            )
        )
            .hasMessageContaining(
                "최신 이력을 다시 확인"
            );

        verify(progressRepository, never())
            .save(any());
        verify(assignment, never())
            .updateLastCompletedLocation(any());
    }
}
