package com.portfolio.warehouse.auth.service;

import com.portfolio.warehouse.auth.domain.EmployeeNumberCounter;
import com.portfolio.warehouse.auth.domain.UserRole;
import com.portfolio.warehouse.auth.repository.EmployeeNumberCounterRepository;
import com.portfolio.warehouse.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeNumberService {

    private final EmployeeNumberCounterRepository counterRepository;

    public EmployeeNumberService(EmployeeNumberCounterRepository counterRepository) {
        this.counterRepository = counterRepository;
    }

    @Transactional
    public String issue(UserRole role) {
        String prefix = switch (role) {
            case ADMIN -> "AD";
            case MATE -> "MT";
        };

        EmployeeNumberCounter counter = counterRepository.findByPrefixForUpdate(prefix)
            .orElseThrow(() -> new BusinessException(
                "EMPLOYEE_COUNTER_NOT_READY",
                "사원번호 카운터가 초기화되지 않았습니다."
            ));

        long sequence = counter.nextValue();
        return prefix + String.format("%04d", sequence);
    }
}
