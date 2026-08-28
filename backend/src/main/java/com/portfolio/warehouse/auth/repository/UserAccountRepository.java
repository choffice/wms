package com.portfolio.warehouse.auth.repository;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.auth.domain.UserRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    boolean existsByRole(UserRole role);
    Optional<UserAccount> findFirstByRole(UserRole role);
}
