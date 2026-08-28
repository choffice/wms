package com.portfolio.warehouse.work.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.common.event.OperationalEventService;
import com.portfolio.warehouse.location.domain.Location;
import com.portfolio.warehouse.location.repository.LocationRepository;
import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.mate.repository.*;
import com.portfolio.warehouse.mate.service.WorkScheduleResolver;
import com.portfolio.warehouse.pda.service.PdaSessionService;
import com.portfolio.warehouse.work.api.dto.*;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WorkProgressUndoTest {

    @Test
    void latestCorrectionUndoAppendsNewCorrectionInsteadOfDeletingHistory() {
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
        WorkProgress latest =
            mock(WorkProgress.class);
        Location current = mock(Location.class);
        Location previous = mock(Location.class);
        Mate mate = mock(Mate.class);
        UserAccount admin = mock(UserAccount.class);

        when(assignmentRepository.findByIdForUpdate(17L))
            .thenReturn(Optional.of(assignment));
        when(assignment.getStatus())
            .thenReturn(WorkAssignmentStatus.IN_PROGRESS);
        when(
            sessionRepository
                .findFirstByWorkAssignmentIdAndEndedAtIsNull(17L)
        ).thenReturn(Optional.empty());

        when(
            progressRepository
                .findFirstByWorkAssignmentIdOrderByReportedAtDescIdDesc(17L)
        ).thenReturn(Optional.of(latest));

        when(latest.getId()).thenReturn(31L);
        when(latest.isCorrection()).thenReturn(true);
        when(latest.getPreviousLocation())
            .thenReturn(previous);
        when(latest.getLastCompletedLocation())
            .thenReturn(current);

        when(current.getId()).thenReturn(88L);
        when(current.getFullCode()).thenReturn("A01-08");
        when(previous.getId()).thenReturn(87L);
        when(previous.getFullCode()).thenReturn("A01-07");

        when(assignment.getCurrentLastCompletedLocation())
            .thenReturn(current);
        when(assignment.getCurrentMate()).thenReturn(mate);
        when(mate.getNickname()).thenReturn("A구역");
        when(currentUserService.account()).thenReturn(admin);

        when(progressRepository.save(any(WorkProgress.class)))
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );
        when(assignment.getId()).thenReturn(17L);

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

        service.undoLatestProgressCorrection(
            17L,
            new AdminUndoProgressCorrectionRequest(
                31L,
                88L,
                "관리자 되돌리기"
            )
        );

        verify(progressRepository).save(
            any(WorkProgress.class)
        );
        verify(progressRepository, never())
            .delete(any());
        verify(assignment)
            .updateLastCompletedLocation(previous);
        verify(eventService).publish(
            eq(com.portfolio.warehouse.log.domain.ActivityType.WORK_PROGRESS_CORRECTION),
            eq(admin),
            eq("A구역"),
            contains("최근 정정 되돌리기"),
            eq("WORK_ASSIGNMENT"),
            eq(17L),
            eq(true),
            eq(true)
        );
    }
}
