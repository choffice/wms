package com.portfolio.warehouse.work.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.log.service.BusinessAuditService;
import com.portfolio.warehouse.work.api.dto.WorkTypeRequest;
import com.portfolio.warehouse.work.domain.WorkType;
import com.portfolio.warehouse.work.repository.WorkTypeRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WorkTypeAuditTest {

    @Test
    void createWritesStructuredMasterDataAudit() {
        WorkTypeRepository repository =
            mock(WorkTypeRepository.class);
        BusinessAuditService auditService =
            mock(BusinessAuditService.class);

        WorkType saved = mock(WorkType.class);

        when(repository.findByName("재고조사"))
            .thenReturn(Optional.empty());
        when(repository.save(any(WorkType.class)))
            .thenReturn(saved);
        when(saved.getId()).thenReturn(7L);
        when(saved.getName()).thenReturn("재고조사");
        when(saved.getDescription()).thenReturn("구역 재고 확인");
        when(saved.isActive()).thenReturn(true);

        WorkTypeService service =
            new WorkTypeService(
                repository,
                auditService
            );

        service.create(
            new WorkTypeRequest(
                "재고조사",
                "구역 재고 확인"
            )
        );

        verify(auditService).record(
            ActivityType.WORK_TYPE_CREATE,
            "재고조사",
            "업무 종류 등록",
            "WORK_TYPE",
            7L
        );
    }
}
