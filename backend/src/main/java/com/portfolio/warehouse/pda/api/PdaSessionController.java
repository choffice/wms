package com.portfolio.warehouse.pda.api;

import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.pda.api.dto.PdaReturnRequest;
import com.portfolio.warehouse.pda.api.dto.PdaUsageResponse;
import com.portfolio.warehouse.pda.domain.PdaReleaseReason;
import com.portfolio.warehouse.pda.repository.PdaUsageHistoryRepository;
import com.portfolio.warehouse.pda.service.PdaSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mate/pda-sessions")
public class PdaSessionController {

    public static final String SESSION_PDA_USAGE_ID = "PDA_USAGE_ID";

    private final PdaSessionService service;
    private final CurrentUserService currentUserService;
    private final PdaUsageHistoryRepository usageRepository;

    public PdaSessionController(
        PdaSessionService service,
        CurrentUserService currentUserService,
        PdaUsageHistoryRepository usageRepository
    ) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.usageRepository = usageRepository;
    }

    @GetMapping("/current")
    public PdaUsageResponse current() {
        Long mateId = currentUserService.mate().getId();
        return service.currentUsageForMate(mateId);
    }

    @PostMapping("/return")
    public PdaUsageResponse release(
        @RequestBody(required = false) PdaReturnRequest request,
        HttpSession session
    ) {
        Long usageId = (Long) session.getAttribute(SESSION_PDA_USAGE_ID);

        if (usageId == null) {
            Long mateId = currentUserService.mate().getId();
            usageId = usageRepository.findFirstByMateIdAndReleasedAtIsNull(mateId)
                .map(history -> history.getId())
                .orElse(null);
        }

        if (usageId == null) return null;

        PdaReleaseReason reason = request == null || request.reason() == null
            ? PdaReleaseReason.RETURNED
            : request.reason();

        PdaUsageResponse response = service.release(usageId, reason);
        session.removeAttribute(SESSION_PDA_USAGE_ID);
        return response;
    }
}
