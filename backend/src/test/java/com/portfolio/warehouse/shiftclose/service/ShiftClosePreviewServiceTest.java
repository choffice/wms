package com.portfolio.warehouse.shiftclose.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.handover.api.dto.*;
import com.portfolio.warehouse.handover.service.HandoverService;
import com.portfolio.warehouse.integrity.api.dto.*;
import com.portfolio.warehouse.integrity.service.IntegrityService;
import com.portfolio.warehouse.issue.repository.SpecialIssueRepository;
import com.portfolio.warehouse.operations.api.dto.*;
import com.portfolio.warehouse.operations.service.OperationsBoardService;
import com.portfolio.warehouse.report.service.ReportService;
import com.portfolio.warehouse.shiftclose.api.dto.ShiftClosePreviewResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShiftClosePreviewServiceTest {

    @Test
    void openSessionAndCriticalIntegrityBecomeBlockers() {
        OperationsBoardService operationsService =
            mock(OperationsBoardService.class);
        HandoverService handoverService =
            mock(HandoverService.class);
        IntegrityService integrityService =
            mock(IntegrityService.class);
        SpecialIssueRepository issueRepository =
            mock(SpecialIssueRepository.class);
        ReportService reportService =
            mock(ReportService.class);

        when(operationsService.board())
            .thenReturn(
                new OperationsBoardResponse(
                    LocalDateTime.now(),
                    new OperationsSummaryResponse(
                        3,
                        1,
                        1,
                        0,
                        0,
                        1,
                        0,
                        1,
                        0,
                        0,
                        0,
                        0
                    ),
                    List.of()
                )
            );

        when(handoverService.board())
            .thenReturn(
                new HandoverBoardResponse(
                    LocalDateTime.now(),
                    new HandoverSummaryResponse(
                        0, 0, 0, 0, 0, 0, 0
                    ),
                    List.of()
                )
            );

        when(integrityService.scan())
            .thenReturn(
                new IntegrityScanResponse(
                    LocalDateTime.now(),
                    new IntegritySummaryResponse(
                        1, 1, 0, 0
                    ),
                    List.of()
                )
            );

        when(
            issueRepository
                .findAllByDeletedAtIsNullOrderByCreatedAtDesc()
        ).thenReturn(List.of());

        when(reportService.recentShiftDates(7))
            .thenReturn(List.of());

        ShiftClosePreviewService service =
            new ShiftClosePreviewService(
                operationsService,
                handoverService,
                integrityService,
                issueRepository,
                reportService
            );

        ShiftClosePreviewResponse result =
            service.preview();

        assertThat(result.summary().blockerCount())
            .isEqualTo(2);
        assertThat(
            result.summary()
                .readyForHandoverReview()
        ).isFalse();

        assertThat(result.checks())
            .filteredOn(item ->
                "BLOCKER".equals(item.level())
            )
            .extracting(
                com.portfolio.warehouse.shiftclose.api.dto.ShiftCloseCheckResponse::code
            )
            .containsExactlyInAnyOrder(
                "OPEN_WORK_SESSION",
                "INTEGRITY_CRITICAL"
            );
    }
}
