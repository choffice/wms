package com.portfolio.warehouse.handover.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.common.event.OperationalEventService;
import com.portfolio.warehouse.handover.api.dto.HandoverNoteRequest;
import com.portfolio.warehouse.handover.domain.HandoverNote;
import com.portfolio.warehouse.handover.repository.HandoverNoteRepository;
import com.portfolio.warehouse.log.domain.ActivityType;
import org.junit.jupiter.api.Test;

class HandoverNoteServiceTest {

    @Test
    void createAppendsNoteAndWritesAuditEvent() {
        HandoverNoteRepository repository =
            mock(HandoverNoteRepository.class);
        CurrentUserService currentUserService =
            mock(CurrentUserService.class);
        OperationalEventService eventService =
            mock(OperationalEventService.class);
        UserAccount admin = mock(UserAccount.class);

        when(currentUserService.account())
            .thenReturn(admin);

        when(repository.save(any(HandoverNote.class)))
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );

        when(admin.getLoginId())
            .thenReturn("AD0001");

        HandoverNoteService service =
            new HandoverNoteService(
                repository,
                currentUserService,
                eventService
            );

        var result =
            service.create(
                new HandoverNoteRequest(
                    java.time.LocalDate.of(
                        2026, 8, 27
                    ),
                    "A01 재고조사 다음 교대 확인"
                )
            );

        assertThat(result.actor())
            .isEqualTo("AD0001");
        assertThat(result.shiftDate())
            .isEqualTo(
                java.time.LocalDate.of(
                    2026, 8, 27
                )
            );

        assertThat(result.content())
            .contains("A01 재고조사");

        verify(repository).save(
            any(HandoverNote.class)
        );
        verify(eventService).publish(
            eq(ActivityType.HANDOVER_NOTE_CREATE),
            eq(admin),
            eq("교대 인계메모"),
            contains("교대 인계메모 등록"),
            eq("HANDOVER_NOTE"),
            isNull(),
            eq(true),
            eq(false)
        );
    }
}
