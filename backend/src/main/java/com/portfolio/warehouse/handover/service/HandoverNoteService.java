package com.portfolio.warehouse.handover.service;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.common.event.OperationalEventService;
import com.portfolio.warehouse.handover.api.dto.*;
import com.portfolio.warehouse.handover.domain.HandoverNote;
import com.portfolio.warehouse.handover.repository.HandoverNoteRepository;
import com.portfolio.warehouse.log.domain.ActivityType;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HandoverNoteService {

    private final HandoverNoteRepository repository;
    private final CurrentUserService currentUserService;
    private final OperationalEventService eventService;

    public HandoverNoteService(
        HandoverNoteRepository repository,
        CurrentUserService currentUserService,
        OperationalEventService eventService
    ) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.eventService = eventService;
    }

    @Transactional
    public HandoverNoteResponse create(
        HandoverNoteRequest request
    ) {
        UserAccount admin =
            currentUserService.account();

        HandoverNote note =
            repository.save(
                new HandoverNote(
                    admin,
                    request.shiftDate(),
                    request.content().trim()
                )
            );

        eventService.publish(
            ActivityType.HANDOVER_NOTE_CREATE,
            admin,
            "교대 인계메모",
            "교대 인계메모 등록 · #"
                + note.getId()
                + (
                    note.getShiftDate() == null
                        ? ""
                        : " / shiftDate "
                            + note.getShiftDate()
                ),
            "HANDOVER_NOTE",
            note.getId(),
            true,
            false
        );

        return HandoverNoteResponse.from(note);
    }

    @Transactional(readOnly = true)
    public List<HandoverNoteResponse> recent() {
        return repository
            .findTop20ByOrderByCreatedAtDescIdDesc()
            .stream()
            .map(HandoverNoteResponse::from)
            .toList();
    }
}
