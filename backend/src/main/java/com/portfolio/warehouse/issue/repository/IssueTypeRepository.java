package com.portfolio.warehouse.issue.repository;

import com.portfolio.warehouse.issue.domain.IssueType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueTypeRepository extends JpaRepository<IssueType, Long> {
    Optional<IssueType> findByName(String name);
}
