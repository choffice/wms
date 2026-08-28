package com.portfolio.warehouse.mate.repository;

import com.portfolio.warehouse.mate.domain.Mate;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MateRepository extends JpaRepository<Mate, Long> {

    Optional<Mate> findByEmployeeNo(String employeeNo);

    boolean existsByEmployeeNo(String employeeNo);

    java.util.List<Mate> findAllByActiveTrueOrderByNicknameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mate m where m.id = :id")
    Optional<Mate> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mate m join fetch m.account where m.employeeNo = :employeeNo")
    Optional<Mate> findByEmployeeNoForUpdate(@Param("employeeNo") String employeeNo);
}
