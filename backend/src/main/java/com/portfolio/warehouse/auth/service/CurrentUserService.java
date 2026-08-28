package com.portfolio.warehouse.auth.service;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.auth.repository.UserAccountRepository;
import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.mate.repository.MateRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {
    private final UserAccountRepository accountRepository;
    private final MateRepository mateRepository;

    public CurrentUserService(
        UserAccountRepository accountRepository,
        MateRepository mateRepository
    ) {
        this.accountRepository = accountRepository;
        this.mateRepository = mateRepository;
    }

    @Transactional(readOnly = true)
    public UserAccount account() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("AUTH_REQUIRED", "로그인이 필요합니다.");
        }
        return accountRepository.findByLoginId(authentication.getName())
            .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Mate mate() {
        UserAccount account = account();
        return mateRepository.findByEmployeeNo(account.getLoginId())
            .orElseThrow(() -> new BusinessException("MATE_PROFILE_NOT_FOUND", "MATE 정보를 찾을 수 없습니다."));
    }
}
