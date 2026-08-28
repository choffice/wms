package com.portfolio.warehouse.pda.repository;

import com.portfolio.warehouse.pda.domain.PdaUsageHistory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PdaUsageHistoryRepository extends JpaRepository<PdaUsageHistory, Long> {

    Optional<PdaUsageHistory> findFirstByPdaDeviceIdAndReleasedAtIsNull(Long pdaDeviceId);
    Optional<PdaUsageHistory> findFirstByMateIdAndReleasedAtIsNull(Long mateId);
    boolean existsByPdaDeviceId(Long pdaDeviceId);
    List<PdaUsageHistory> findAllByPdaDeviceIdOrderByAssignedAtDesc(Long pdaDeviceId);

    @Query("""
        select p
        from PdaUsageHistory p
        join fetch p.pdaDevice
        join fetch p.mate
        where p.assignedAt < :to
          and (p.releasedAt is null or p.releasedAt >= :from)
        order by p.assignedAt asc
        """)
    List<PdaUsageHistory> findOverlapping(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}
