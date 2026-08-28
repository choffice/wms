package com.portfolio.warehouse.auth.service;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.auth.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class WarehouseUserDetailsService implements UserDetailsService {
    private final UserAccountRepository repository;

    public WarehouseUserDetailsService(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = repository.findByLoginId(username)
            .orElseThrow(() -> new UsernameNotFoundException("계정을 찾을 수 없습니다."));

        return User.builder()
            .username(account.getLoginId())
            .password(account.getPasswordHash())
            .disabled(!account.isEnabled())
            .roles(account.getRole().name())
            .build();
    }
}
