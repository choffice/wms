package com.portfolio.warehouse.actionqueue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.actionqueue.api.dto.ActionQueueResponse;
import com.portfolio.warehouse.handover.api.dto.*;
import com.portfolio.warehouse.handover.service.HandoverService;
import com.portfolio.warehouse.integrity.api.dto.*;
import com.portfolio.warehouse.integrity.service.IntegrityService;
import com.portfolio.warehouse.issue.repository.SpecialIssueRepository;
import com.portfolio.warehouse.operations.api.dto.*;
import com.portfolio.warehouse.operations.service.OperationsBoardService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ActionQueueServiceTest {

    @Test
    void blockerIsSortedBeforeHandoverWithoutBusinessPriorityScoring() {
        OperationsBoardService operations =
            mock(OperationsBoardService.class);
        HandoverService handover =
            mock(HandoverService.class);
        IntegrityService integrity =
            mock(IntegrityService.class);
        SpecialIssueRepository issues =
            mock(SpecialIssueRepository.class);

        when(operations.board())
            .thenReturn(
                new OperationsBoardResponse(
                    LocalDateTime.now(),
                    new OperationsSummaryResponse(
                        0, 0, 0, 0, 0,
                        0, 0, 0, 0,
                        0, 0, 0
                    ),
                    List.of()
                )
            );

        when(integrity.scan())
            .thenReturn(
                new IntegrityScanResponse(
                    LocalDateTime.now(),
                    new IntegritySummaryResponse(
                        1, 1, 0, 0
                    ),
                    List.of(
                        new IntegrityIssueResponse(
                            "X:MATE:3",
                            "CRITICAL",
                            "TEST_CRITICAL",
                            "MATE",
                            3L,
                            "MATE A",
                            "정합성 치명 테스트",
                            null
                        )
                    )
                )
            );

        when(handover.board())
            .thenReturn(
                new HandoverBoardResponse(
                    LocalDateTime.now(),
                    new HandoverSummaryResponse(
                        1, 1, 0, 0, 0, 1, 0
                    ),
                    List.of(
                        new HandoverRowResponse(
                            17L,
                            "IN_PROGRESS",
                            "OFF_DUTY_HANDOVER",
                            "퇴근 인수인계",
                            1L,
                            "재고조사",
                            2L,
                            "A01",
                            3L,
                            "A01-01",
                            4L,
                            "A01-08",
                            5L,
                            "MT0005",
                            "A구역",
                            "OFF_DUTY",
                            "퇴근",
                            null,
                            10L,
                            LocalDateTime.now().minusHours(1),
                            LocalDateTime.now(),
                            "SCHEDULE_END",
                            "NORMAL",
                            false,
                            true
                        )
                    )
                )
            );

        when(
            issues.findAllByDeletedAtIsNullOrderByCreatedAtDesc()
        ).thenReturn(List.of());

        ActionQueueService service =
            new ActionQueueService(
                operations,
                handover,
                integrity,
                issues
            );

        ActionQueueResponse result =
            service.queue();

        assertThat(result.summary().totalCount())
            .isEqualTo(2);
        assertThat(result.items().get(0).level())
            .isEqualTo("BLOCKER");
        assertThat(result.items().get(1).level())
            .isEqualTo("HANDOVER");
    }
}
