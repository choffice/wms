package com.portfolio.warehouse.integrity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.integrity.api.dto.*;
import com.portfolio.warehouse.log.service.BusinessAuditService;
import com.portfolio.warehouse.mate.domain.*;
import com.portfolio.warehouse.mate.repository.*;
import com.portfolio.warehouse.pda.domain.*;
import com.portfolio.warehouse.pda.repository.*;
import com.portfolio.warehouse.pda.service.PdaSessionService;
import com.portfolio.warehouse.work.repository.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IntegrityServiceTest {

    @Test
    void scanFindsRepairablePdaAndMateStateMismatches() {
        PdaDeviceRepository deviceRepository =
            mock(PdaDeviceRepository.class);
        PdaUsageHistoryRepository usageRepository =
            mock(PdaUsageHistoryRepository.class);
        MateRepository mateRepository =
            mock(MateRepository.class);
        MateStatusHistoryRepository statusHistoryRepository =
            mock(MateStatusHistoryRepository.class);
        WorkSessionRepository sessionRepository =
            mock(WorkSessionRepository.class);
        WorkAssignmentRepository assignmentRepository =
            mock(WorkAssignmentRepository.class);
        PdaSessionService pdaSessionService =
            mock(PdaSessionService.class);
        BusinessAuditService auditService =
            mock(BusinessAuditService.class);

        PdaDevice device = mock(PdaDevice.class);
        Mate mate = mock(Mate.class);

        when(device.getId()).thenReturn(10L);
        when(device.getDeviceNumber()).thenReturn(32);
        when(device.getStatus()).thenReturn(PdaStatus.IN_USE);
        when(device.isActive()).thenReturn(true);

        when(mate.getId()).thenReturn(20L);
        when(mate.getNickname()).thenReturn("A구역");
        when(mate.getCurrentStatus())
            .thenReturn(MateStatus.WORKING);
        when(mate.isActive()).thenReturn(true);

        when(deviceRepository.findAll())
            .thenReturn(List.of(device));
        when(usageRepository.findAll())
            .thenReturn(List.of());
        when(mateRepository.findAll())
            .thenReturn(List.of(mate));
        when(sessionRepository.findAllByEndedAtIsNull())
            .thenReturn(List.of());

        IntegrityService service =
            new IntegrityService(
                deviceRepository,
                usageRepository,
                mateRepository,
                statusHistoryRepository,
                sessionRepository,
                assignmentRepository,
                pdaSessionService,
                auditService
            );

        IntegrityScanResponse result = service.scan();

        assertThat(result.summary().total()).isEqualTo(2);
        assertThat(result.summary().repairable()).isEqualTo(2);

        assertThat(result.issues())
            .extracting(IntegrityIssueResponse::code)
            .containsExactlyInAnyOrder(
                "PDA_IN_USE_WITHOUT_USAGE",
                "MATE_WORKING_WITHOUT_SESSION"
            );
    }

    @Test
    void repairingOrphanPdaOnlyChangesStatusWhenNoActiveUsageExists() {
        PdaDeviceRepository deviceRepository =
            mock(PdaDeviceRepository.class);
        PdaUsageHistoryRepository usageRepository =
            mock(PdaUsageHistoryRepository.class);
        MateRepository mateRepository =
            mock(MateRepository.class);
        MateStatusHistoryRepository statusHistoryRepository =
            mock(MateStatusHistoryRepository.class);
        WorkSessionRepository sessionRepository =
            mock(WorkSessionRepository.class);
        WorkAssignmentRepository assignmentRepository =
            mock(WorkAssignmentRepository.class);
        PdaSessionService pdaSessionService =
            mock(PdaSessionService.class);
        BusinessAuditService auditService =
            mock(BusinessAuditService.class);

        PdaDevice device = mock(PdaDevice.class);

        when(deviceRepository.findByIdForUpdate(10L))
            .thenReturn(Optional.of(device));
        when(device.getId()).thenReturn(10L);
        when(device.getDeviceNumber()).thenReturn(32);
        when(device.getStatus()).thenReturn(PdaStatus.IN_USE);
        when(
            usageRepository
                .findFirstByPdaDeviceIdAndReleasedAtIsNull(10L)
        ).thenReturn(Optional.empty());

        IntegrityService service =
            new IntegrityService(
                deviceRepository,
                usageRepository,
                mateRepository,
                statusHistoryRepository,
                sessionRepository,
                assignmentRepository,
                pdaSessionService,
                auditService
            );

        IntegrityRepairResult result =
            service.repair(
                new IntegrityRepairRequest(
                    IntegrityService.RESET_ORPHAN_PDA_STATUS,
                    10L
                )
            );

        assertThat(result.repairedCount()).isEqualTo(1);
        verify(device).changeStatus(PdaStatus.AVAILABLE);
        verify(auditService).record(
            eq(com.portfolio.warehouse.log.domain.ActivityType.INTEGRITY_REPAIR),
            eq("PDA 32"),
            contains("IN_USE → AVAILABLE"),
            eq("PDA_DEVICE"),
            eq(10L)
        );
    }
}
