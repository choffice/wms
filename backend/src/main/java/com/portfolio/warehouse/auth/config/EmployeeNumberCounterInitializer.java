package com.portfolio.warehouse.auth.config;

import com.portfolio.warehouse.auth.domain.EmployeeNumberCounter;
import com.portfolio.warehouse.auth.repository.EmployeeNumberCounterRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
public class EmployeeNumberCounterInitializer implements ApplicationRunner {
    private final EmployeeNumberCounterRepository repository;

    public EmployeeNumberCounterInitializer(EmployeeNumberCounterRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String prefix : List.of("AD", "MT")) {
            if (!repository.existsById(prefix)) {
                repository.save(new EmployeeNumberCounter(prefix));
            }
        }
    }
}
