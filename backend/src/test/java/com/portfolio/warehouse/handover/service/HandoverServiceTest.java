package com.portfolio.warehouse.handover.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.handover.api.dto.HandoverBoardResponse;
import com.portfolio.warehouse.location.domain.Location;
import com.portfolio.warehouse.mate.domain.*;
import com.portfolio.warehouse.pda.repository.PdaUsageHistoryRepository;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class HandoverServiceTest {

    @Test
    void offDutyAssignmentWithoutOpenSessionAppearsAsHandoverCandidate() {
        WorkAssignmentRepository assignmentRepository =
            mock(WorkAssignmentRepository.class);
        WorkSessionRepository sessionRepository =
            mock(WorkSessionRepository.class);
        PdaUsageHistoryRepository usageRepository =
            mock(PdaUsageHistoryRepository.class);

        WorkAssignment assignment =
            mock(WorkAssignment.class);
        Mate mate = mock(Mate.class);
        WorkType workType = mock(WorkType.class);
        Location area = mock(Location.class);
        Location start = mock(Location.class);
        Location lastLocation = mock(Location.class);
        WorkSession lastSession = mock(WorkSession.class);

        when(assignment.getId()).thenReturn(17L);
        when(assignment.getStatus())
            .thenReturn(WorkAssignmentStatus.IN_PROGRESS);
        when(assignment.getCurrentMate()).thenReturn(mate);
        when(assignment.getWorkType()).thenReturn(workType);
        when(assignment.getAreaLocation()).thenReturn(area);
        when(assignment.getStartLocation()).thenReturn(start);
        when(assignment.getCurrentLastCompletedLocation())
            .thenReturn(lastLocation);

        when(mate.getId()).thenReturn(3L);
        when(mate.getEmployeeNo()).thenReturn("MT0003");
        when(mate.getNickname()).thenReturn("A구역");
        when(mate.getCurrentStatus())
            .thenReturn(MateStatus.OFF_DUTY);
        when(mate.getCurrentWhereabouts()).thenReturn("퇴근");

        when(workType.getId()).thenReturn(4L);
        when(workType.getName()).thenReturn("재고조사");

        when(area.getId()).thenReturn(5L);
        when(area.getFullCode()).thenReturn("A01");
        when(start.getId()).thenReturn(6L);
        when(start.getFullCode()).thenReturn("A01-01");
        when(lastLocation.getId()).thenReturn(7L);
        when(lastLocation.getFullCode()).thenReturn("A01-08");

        when(lastSession.getId()).thenReturn(9L);
        when(lastSession.getStartedAt()).thenReturn(
            LocalDateTime.of(2026, 8, 27, 9, 0)
        );
        when(lastSession.getEndedAt()).thenReturn(
            LocalDateTime.of(2026, 8, 27, 18, 0)
        );
        when(lastSession.getEndReason())
            .thenReturn(WorkSessionEndReason.SCHEDULE_END);
        when(lastSession.getQualityStatus())
            .thenReturn(WorkSessionQualityStatus.NORMAL);

        when(sessionRepository.findAllByEndedAtIsNull())
            .thenReturn(List.of());
        when(assignmentRepository.findAllByOrderByAssignedAtDesc())
            .thenReturn(List.of(assignment));
        when(
            sessionRepository
                .findAllByWorkAssignmentIdOrderByStartedAtAsc(17L)
        ).thenReturn(List.of(lastSession));
        when(usageRepository.findAll())
            .thenReturn(List.of());

        HandoverService service =
            new HandoverService(
                assignmentRepository,
                sessionRepository,
                usageRepository
            );

        HandoverBoardResponse result = service.board();

        assertThat(result.summary().pendingCount())
            .isEqualTo(1);
        assertThat(result.summary().handoverCandidateCount())
            .isEqualTo(1);
        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0).handoverState())
            .isEqualTo("OFF_DUTY_HANDOVER");
        assertThat(
            result.rows().get(0)
                .currentLastCompletedLocation()
        ).isEqualTo("A01-08");
    }
}
