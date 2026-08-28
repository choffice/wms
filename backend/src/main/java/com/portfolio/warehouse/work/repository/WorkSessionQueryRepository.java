package com.portfolio.warehouse.work.repository;

import com.portfolio.warehouse.work.domain.*;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class WorkSessionQueryRepository {

    private final JPAQueryFactory queryFactory;

    public WorkSessionQueryRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<WorkSession> search(
        LocalDateTime from,
        LocalDateTime to,
        Long workTypeId,
        Long mateId,
        WorkSessionQualityStatus qualityStatus
    ) {
        QWorkSession session = QWorkSession.workSession;
        BooleanBuilder where = new BooleanBuilder();

        if (from != null) {
            where.and(session.endedAt.isNull().or(session.endedAt.gt(from)));
        }

        if (to != null) {
            where.and(session.startedAt.lt(to));
        }

        if (workTypeId != null) {
            where.and(session.workAssignment.workType.id.eq(workTypeId));
        }

        if (mateId != null) {
            where.and(session.mate.id.eq(mateId));
        }

        if (qualityStatus != null) {
            where.and(session.qualityStatus.eq(qualityStatus));
        }

        return queryFactory
            .selectFrom(session)
            .join(session.workAssignment).fetchJoin()
            .join(session.workAssignment.workType).fetchJoin()
            .join(session.mate).fetchJoin()
            .where(where)
            .orderBy(session.startedAt.asc())
            .fetch();
    }
}
