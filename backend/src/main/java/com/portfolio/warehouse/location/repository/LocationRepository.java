package com.portfolio.warehouse.location.repository;

import com.portfolio.warehouse.location.domain.Location;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByFullCode(String fullCode);

    boolean existsByFullCode(String fullCode);

    boolean existsByParentIdAndSegment(Long parentId, String segment);

    boolean existsByParentIsNullAndSegment(String segment);

    List<Location> findAllByParentIdOrderByFullCodeAsc(Long parentId);

    List<Location> findAllByFullCodeStartingWithOrderByFullCodeAsc(String prefix);

    List<Location> findAllByOrderByFullCodeAsc();
}
