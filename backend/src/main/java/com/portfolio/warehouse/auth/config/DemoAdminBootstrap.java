package com.portfolio.warehouse.auth.config;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.auth.domain.UserRole;
import com.portfolio.warehouse.auth.repository.UserAccountRepository;
import com.portfolio.warehouse.auth.service.EmployeeNumberService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
public class DemoAdminBootstrap implements ApplicationRunner {
    private final UserAccountRepository accountRepository;
    private final EmployeeNumberService employeeNumberService;
    private final PasswordEncoder passwordEncoder;

    @Value("${warehouse.demo-admin.password:admin1234}")
    private String demoAdminPassword;

    public DemoAdminBootstrap(
        UserAccountRepository accountRepository,
        EmployeeNumberService employeeNumberService,
        PasswordEncoder passwordEncoder
    ) {
        this.accountRepository = accountRepository;
        this.employeeNumberService = employeeNumberService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (accountRepository.existsByRole(UserRole.ADMIN)) return;

        String employeeNo = employeeNumberService.issue(UserRole.ADMIN);
        accountRepository.save(
            new UserAccount(employeeNo, passwordEncoder.encode(demoAdminPassword), UserRole.ADMIN)
        );
    }
}
