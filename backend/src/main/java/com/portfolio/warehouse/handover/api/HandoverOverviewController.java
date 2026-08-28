package com.portfolio.warehouse.handover.api;

import com.portfolio.warehouse.handover.api.dto.*;
import com.portfolio.warehouse.handover.service.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class HandoverOverviewController {

    private final HandoverOverviewService overviewService;
    private final HandoverNoteService noteService;

    public HandoverOverviewController(
        HandoverOverviewService overviewService,
        HandoverNoteService noteService
    ) {
        this.overviewService = overviewService;
        this.noteService = noteService;
    }

    @GetMapping("/handover-overview")
    public HandoverOverviewResponse overview() {
        return overviewService.overview();
    }

    @GetMapping("/handover-notes")
    public List<HandoverNoteResponse> notes() {
        return noteService.recent();
    }

    @PostMapping("/handover-notes")
    @ResponseStatus(HttpStatus.CREATED)
    public HandoverNoteResponse createNote(
        @Valid @RequestBody
        HandoverNoteRequest request
    ) {
        return noteService.create(request);
    }
}
