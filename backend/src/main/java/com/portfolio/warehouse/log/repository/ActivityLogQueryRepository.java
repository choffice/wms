package com.portfolio.warehouse.log.repository;

import com.portfolio.warehouse.auth.domain.QUserAccount;
import com.portfolio.warehouse.log.domain.*;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityLogQueryRepository {

    private final JPAQueryFactory queryFactory;

    public ActivityLogQueryRepository(
        JPAQueryFactory queryFactory
    ) {
        this.queryFactory = queryFactory;
    }

    public List<ActivityLog> search(
        LocalDateTime from,
        LocalDateTime to,
        ActivityType type,
        String actorText,
        String referenceType,
        Long referenceId,
        String keyword,
        int page,
        int size
    ) {
        QActivityLog log = QActivityLog.activityLog;
        QUserAccount actor = new QUserAccount("auditActor");

        BooleanBuilder where = where(
            log,
            actor,
            from,
            to,
            type,
            actorText,
            referenceType,
            referenceId,
            keyword
        );

        return queryFactory
            .selectFrom(log)
            .leftJoin(log.actorAccount, actor).fetchJoin()
            .where(where)
            .orderBy(
                log.createdAt.desc(),
                log.id.desc()
            )
            .offset((long) page * size)
            .limit(size)
            .fetch();
    }


    public List<ActivityLog> latestByActorRole(
        com.portfolio.warehouse.auth.domain.UserRole role,
        int limit
    ) {
        QActivityLog log = QActivityLog.activityLog;
        QUserAccount actor =
            new QUserAccount("latestRoleActor");

        return queryFactory
            .selectFrom(log)
            .join(log.actorAccount, actor).fetchJoin()
            .where(actor.role.eq(role))
            .orderBy(
                log.createdAt.desc(),
                log.id.desc()
            )
            .limit(Math.max(1, Math.min(100, limit)))
            .fetch();
    }

    public long count(
        LocalDateTime from,
        LocalDateTime to,
        ActivityType type,
        String actorText,
        String referenceType,
        Long referenceId,
        String keyword
    ) {
        QActivityLog log = QActivityLog.activityLog;
        QUserAccount actor = new QUserAccount("auditActorCount");

        BooleanBuilder where = where(
            log,
            actor,
            from,
            to,
            type,
            actorText,
            referenceType,
            referenceId,
            keyword
        );

        Long count = queryFactory
            .select(log.count())
            .from(log)
            .leftJoin(log.actorAccount, actor)
            .where(where)
            .fetchOne();

        return count == null ? 0L : count;
    }

    private BooleanBuilder where(
        QActivityLog log,
        QUserAccount actor,
        LocalDateTime from,
        LocalDateTime to,
        ActivityType type,
        String actorText,
        String referenceType,
        Long referenceId,
        String keyword
    ) {
        BooleanBuilder where = new BooleanBuilder();

        if (from != null) {
            where.and(log.createdAt.goe(from));
        }

        if (to != null) {
            where.and(log.createdAt.lt(to));
        }

        if (type != null) {
            where.and(log.type.eq(type));
        }

        if (
            actorText != null
                && !actorText.isBlank()
        ) {
            where.and(
                actor.loginId.containsIgnoreCase(
                    actorText.trim()
                )
            );
        }

        if (
            referenceType != null
                && !referenceType.isBlank()
        ) {
            where.and(
                log.referenceType.eq(
                    referenceType.trim()
                )
            );
        }

        if (referenceId != null) {
            where.and(
                log.referenceId.eq(referenceId)
            );
        }

        if (
            keyword != null
                && !keyword.isBlank()
        ) {
            String q = keyword.trim();

            where.and(
                log.message.containsIgnoreCase(q)
                    .or(log.targetLabel.containsIgnoreCase(q))
                    .or(log.referenceType.containsIgnoreCase(q))
                    .or(actor.loginId.containsIgnoreCase(q))
            );
        }

        return where;
    }
}
