package com.portfolio.warehouse.auth.api;

import com.portfolio.warehouse.auth.api.dto.*;
import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.auth.domain.UserRole;
import com.portfolio.warehouse.auth.repository.UserAccountRepository;
import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.common.event.OperationalEventService;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.mate.repository.MateRepository;
import com.portfolio.warehouse.pda.api.PdaSessionController;
import com.portfolio.warehouse.pda.api.dto.PdaUsageResponse;
import com.portfolio.warehouse.pda.domain.PdaReleaseReason;
import com.portfolio.warehouse.pda.repository.PdaUsageHistoryRepository;
import com.portfolio.warehouse.pda.repository.PdaDeviceRepository;
import com.portfolio.warehouse.pda.domain.PdaStatus;
import java.util.List;
import com.portfolio.warehouse.pda.service.PdaSessionService;
import com.portfolio.warehouse.work.service.MateLogoutWorkService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserAccountRepository accountRepository;
    private final MateRepository mateRepository;
    private final PdaSessionService pdaSessionService;
    private final PdaUsageHistoryRepository pdaUsageRepository;
    private final MateLogoutWorkService mateLogoutWorkService;
    private final PdaDeviceRepository pdaDeviceRepository;
    private final OperationalEventService eventService;

    public AuthController(
        AuthenticationManager authenticationManager,
        SecurityContextRepository securityContextRepository,
        UserAccountRepository accountRepository,
        MateRepository mateRepository,
        PdaSessionService pdaSessionService,
        PdaUsageHistoryRepository pdaUsageRepository,
        MateLogoutWorkService mateLogoutWorkService,
        PdaDeviceRepository pdaDeviceRepository,
        OperationalEventService eventService
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.accountRepository = accountRepository;
        this.mateRepository = mateRepository;
        this.pdaSessionService = pdaSessionService;
        this.pdaUsageRepository = pdaUsageRepository;
        this.mateLogoutWorkService = mateLogoutWorkService;
        this.pdaDeviceRepository = pdaDeviceRepository;
        this.eventService = eventService;
    }

    @PostMapping("/admin/login")
    public AuthMeResponse adminLogin(
        @Valid @RequestBody AdminLoginRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        Authentication authentication = authenticate(request.employeeNo(), request.password());
        UserAccount account = account(request.employeeNo());

        if (account.getRole() != UserRole.ADMIN) {
            throw new BusinessException("ADMIN_ONLY", "관리자 계정이 아닙니다.");
        }

        saveAuthentication(authentication, servletRequest, servletResponse);

        eventService.publish(
            ActivityType.ADMIN_LOGIN,
            account,
            "SYSTEM",
            "관리자 로그인",
            "AUTH",
            account.getId(),
            false,
            false
        );

        return new AuthMeResponse(
            account.getLoginId(),
            account.getRole(),
            null,
            null,
            null,
            null
        );
    }

    @GetMapping("/mate/pdas")
    public List<PdaLoginOptionResponse> matePdaOptions() {
        return pdaDeviceRepository.findAll().stream()
            .filter(device -> device.isActive())
            .filter(device -> device.getStatus() == PdaStatus.AVAILABLE)
            .sorted(
                java.util.Comparator.comparing(
                    (com.portfolio.warehouse.pda.domain.PdaDevice device) ->
                        device.getDeviceNumber()
                )
            )
            .map(device -> new PdaLoginOptionResponse(
                device.getDeviceNumber(),
                device.getStatus().name()
            ))
            .toList();
    }

    @PostMapping("/mate/login")
    public AuthMeResponse mateLogin(
        @Valid @RequestBody MateLoginRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        Authentication authentication = authenticate(request.employeeNo(), request.password());
        UserAccount account = account(request.employeeNo());

        if (account.getRole() != UserRole.MATE) {
            throw new BusinessException("MATE_ONLY", "MATE 계정이 아닙니다.");
        }

        PdaUsageResponse usage = pdaSessionService.allocate(
            request.deviceNumber(),
            request.employeeNo()
        );

        saveAuthentication(authentication, servletRequest, servletResponse);
        servletRequest.getSession(true).setAttribute(
            PdaSessionController.SESSION_PDA_USAGE_ID,
            usage.usageId()
        );

        Mate mate = mateRepository.findByEmployeeNo(request.employeeNo())
            .orElseThrow(() -> new BusinessException(
                "MATE_PROFILE_NOT_FOUND",
                "MATE 정보를 찾을 수 없습니다."
            ));

        eventService.publish(
            ActivityType.MATE_LOGIN,
            account,
            "관리자",
            "MATE 로그인 · PDA " + usage.deviceNumber(),
            "AUTH",
            account.getId(),
            true,
            false
        );

        return new AuthMeResponse(
            account.getLoginId(),
            account.getRole(),
            mate.getId(),
            mate.getNickname(),
            usage.usageId(),
            usage.deviceNumber()
        );
    }

    @GetMapping("/me")
    public AuthMeResponse me(Authentication authentication, HttpSession session) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) return null;

        UserAccount account = account(authentication.getName());
        if (account.getRole() == UserRole.ADMIN) {
            return new AuthMeResponse(account.getLoginId(), account.getRole(), null, null, null, null);
        }

        Mate mate = mateRepository.findByEmployeeNo(account.getLoginId())
            .orElseThrow(() -> new BusinessException("MATE_PROFILE_NOT_FOUND", "MATE 정보를 찾을 수 없습니다."));

        PdaUsageResponse currentUsage =
            pdaSessionService.currentUsageForMate(mate.getId());

        Long usageId = currentUsage == null ? null : currentUsage.usageId();
        Integer pdaNumber = currentUsage == null ? null : currentUsage.deviceNumber();

        if (usageId == null) {
            session.removeAttribute(PdaSessionController.SESSION_PDA_USAGE_ID);
        } else {
            session.setAttribute(PdaSessionController.SESSION_PDA_USAGE_ID, usageId);
        }

        return new AuthMeResponse(
            account.getLoginId(),
            account.getRole(),
            mate.getId(),
            mate.getNickname(),
            usageId,
            pdaNumber
        );
    }

    @PostMapping("/logout")
    public void logout(Authentication authentication, HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (authentication != null && authentication.isAuthenticated()) {
            UserAccount account = accountRepository
                .findByLoginId(authentication.getName())
                .orElse(null);

            if (account != null) {
                eventService.publish(
                    ActivityType.AUTH_LOGOUT,
                    account,
                    "SYSTEM",
                    account.getRole() == UserRole.ADMIN
                        ? "관리자 로그아웃"
                        : "MATE 로그아웃",
                    "AUTH",
                    account.getId(),
                    account.getRole() == UserRole.MATE,
                    false
                );

                if (account.getRole() == UserRole.MATE) {
                    mateLogoutWorkService
                        .closeActiveWorkForLogout(
                            account.getLoginId()
                        );
                }
            }
        }

        if (session != null) {
            Long usageId = (Long) session.getAttribute(PdaSessionController.SESSION_PDA_USAGE_ID);
            if (usageId != null) {
                pdaSessionService.release(usageId, PdaReleaseReason.LOGOUT);
            }
        }

        SecurityContextHolder.clearContext();
        if (session != null) session.invalidate();
    }

    private Authentication authenticate(String username, String password) {
        return authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password)
        );
    }

    private UserAccount account(String loginId) {
        return accountRepository.findByLoginId(loginId)
            .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다."));
    }

    private void saveAuthentication(
        Authentication authentication,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        request.getSession(true);
        request.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
