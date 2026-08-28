package com.portfolio.warehouse.issue.repository;

import com.portfolio.warehouse.issue.domain.*;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SpecialIssueRepository extends JpaRepository<SpecialIssue, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from SpecialIssue i where i.id = :id")
    Optional<SpecialIssue> findByIdForUpdate(@Param("id") Long id);
    List<SpecialIssue> findAllByDeletedAtIsNullOrderByCreatedAtDesc();
    List<SpecialIssue> findAllByDeletedAtIsNullAndStatusOrderByCreatedAtDesc(IssueStatus status);
    List<SpecialIssue> findAllByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
        LocalDateTime from,
        LocalDateTime to
    );
}
