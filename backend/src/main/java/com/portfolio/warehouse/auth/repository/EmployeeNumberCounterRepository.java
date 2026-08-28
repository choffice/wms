package com.portfolio.warehouse.auth.repository;

import com.portfolio.warehouse.auth.domain.EmployeeNumberCounter;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeNumberCounterRepository extends JpaRepository<EmployeeNumberCounter, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from EmployeeNumberCounter c where c.prefix = :prefix")
    Optional<EmployeeNumberCounter> findByPrefixForUpdate(@Param("prefix") String prefix);
}
