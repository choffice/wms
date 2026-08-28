package com.portfolio.warehouse.pda.repository;

import com.portfolio.warehouse.pda.domain.PdaDevice;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PdaDeviceRepository extends JpaRepository<PdaDevice, Long> {

    Optional<PdaDevice> findByDeviceNumber(Integer deviceNumber);

    boolean existsByDeviceNumber(Integer deviceNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PdaDevice p where p.id = :id")
    Optional<PdaDevice> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PdaDevice p where p.deviceNumber = :deviceNumber")
    Optional<PdaDevice> findByDeviceNumberForUpdate(@Param("deviceNumber") Integer deviceNumber);
}
