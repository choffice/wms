package com.portfolio.warehouse.readiness.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.handover.api.dto.*;
import com.portfolio.warehouse.handover.service.HandoverService;
import com.portfolio.warehouse.integrity.api.dto.*;
import com.portfolio.warehouse.integrity.service.IntegrityService;
import com.portfolio.warehouse.issue.repository.*;
import com.portfolio.warehouse.location.repository.LocationRepository;
import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.mate.repository.MateRepository;
import com.portfolio.warehouse.pda.domain.PdaDevice;
import com.portfolio.warehouse.pda.repository.PdaDeviceRepository;
import com.portfolio.warehouse.work.domain.WorkType;
import com.portfolio.warehouse.work.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SystemReadinessServiceTest {

    @Test
    void masterDataAndZeroCriticalIntegrityAreEnoughForDemoReady() {
        MateRepository mates = mock(MateRepository.class);
        PdaDeviceRepository pdas = mock(PdaDeviceRepository.class);
        LocationRepository locations = mock(LocationRepository.class);
        WorkTypeRepository workTypes = mock(WorkTypeRepository.class);
        IssueTypeRepository issueTypes = mock(IssueTypeRepository.class);
        WorkSessionRepository sessions = mock(WorkSessionRepository.class);
        SpecialIssueRepository issues = mock(SpecialIssueRepository.class);
        IntegrityService integrity = mock(IntegrityService.class);
        HandoverService handover = mock(HandoverService.class);

        Mate mate = mock(Mate.class);
        PdaDevice pda = mock(PdaDevice.class);
        com.portfolio.warehouse.location.domain.Location location =
            mock(com.portfolio.warehouse.location.domain.Location.class);
        WorkType workType = mock(WorkType.class);
        com.portfolio.warehouse.issue.domain.IssueType issueType =
            mock(com.portfolio.warehouse.issue.domain.IssueType.class);

        when(mates.findAllByActiveTrueOrderByNicknameAsc())
            .thenReturn(List.of(mate));
        when(pdas.findAll()).thenReturn(List.of(pda));
        when(pda.isActive()).thenReturn(true);
        when(locations.findAll()).thenReturn(List.of(location));
        when(location.isActive()).thenReturn(true);
        when(workTypes.findAll()).thenReturn(List.of(workType));
        when(workType.isActive()).thenReturn(true);
        when(issueTypes.findAll()).thenReturn(List.of(issueType));
        when(issueType.isActive()).thenReturn(true);
        when(sessions.findAllByEndedAtIsNull())
            .thenReturn(List.of());
        when(issues.findAllByDeletedAtIsNullOrderByCreatedAtDesc())
            .thenReturn(List.of());

        when(integrity.scan())
            .thenReturn(
                new IntegrityScanResponse(
                    LocalDateTime.now(),
                    new IntegritySummaryResponse(
                        0, 0, 0, 0
                    ),
                    List.of()
                )
            );

        when(handover.board())
            .thenReturn(
                new HandoverBoardResponse(
                    LocalDateTime.now(),
                    new HandoverSummaryResponse(
                        0, 0, 0, 0, 0, 0, 0
                    ),
                    List.of()
                )
            );

        SystemReadinessService service =
            new SystemReadinessService(
                mates,
                pdas,
                locations,
                workTypes,
                issueTypes,
                sessions,
                issues,
                integrity,
                handover
            );

        var result = service.readiness();

        assertThat(result.readyForDemo()).isTrue();
        assertThat(result.csrfEnabled()).isTrue();
        assertThat(result.counts().integrityCritical())
            .isZero();
    }
}
