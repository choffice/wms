package com.portfolio.warehouse.report.service;

import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.issue.domain.SpecialIssue;
import com.portfolio.warehouse.issue.repository.SpecialIssueRepository;
import com.portfolio.warehouse.mate.service.WorkScheduleResolver;
import com.portfolio.warehouse.pda.domain.PdaUsageHistory;
import com.portfolio.warehouse.pda.repository.PdaUsageHistoryRepository;
import com.portfolio.warehouse.report.api.dto.*;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.repository.WorkSessionQueryRepository;
import com.portfolio.warehouse.work.repository.WorkProgressRepository;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private static final int MAX_RANGE_DAYS = 366;

    private final WorkSessionQueryRepository sessionQueryRepository;
    private final PdaUsageHistoryRepository pdaUsageRepository;
    private final SpecialIssueRepository issueRepository;
    private final WorkProgressRepository progressRepository;
    private final WorkScheduleResolver scheduleResolver;

    public ReportService(
        WorkSessionQueryRepository sessionQueryRepository,
        PdaUsageHistoryRepository pdaUsageRepository,
        SpecialIssueRepository issueRepository,
        WorkProgressRepository progressRepository,
        WorkScheduleResolver scheduleResolver
    ) {
        this.sessionQueryRepository = sessionQueryRepository;
        this.pdaUsageRepository = pdaUsageRepository;
        this.issueRepository = issueRepository;
        this.progressRepository = progressRepository;
        this.scheduleResolver = scheduleResolver;
    }

    @Transactional(readOnly = true)
    public List<WorkTimeStatResponse> workTypeStats(
        LocalDate fromDate,
        LocalDate toDate,
        Long mateId,
        Long workTypeId,
        boolean includeUncertain
    ) {
        LocalDateTime from =
            fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime to =
            toDate == null
                ? null
                : toDate.plusDays(1).atStartOfDay();

        validateRange(fromDate, toDate);

        WorkSessionQualityStatus quality =
            includeUncertain
                ? null
                : WorkSessionQualityStatus.NORMAL;

        List<WorkSession> sessions =
            sessionQueryRepository.search(
                from,
                to,
                workTypeId,
                mateId,
                quality
            ).stream()
                .filter(session -> session.getEndedAt() != null)
                .toList();

        Map<String, List<WorkSession>> grouped =
            sessions.stream()
                .collect(
                    Collectors.groupingBy(
                        session ->
                            session
                                .getWorkAssignment()
                                .getWorkType()
                                .getName()
                    )
                );

        return grouped.entrySet().stream()
            .map(entry -> {
                long count = entry.getValue().size();

                long total = entry.getValue().stream()
                    .mapToLong(session ->
                        clippedSeconds(
                            session,
                            from,
                            to
                        )
                    )
                    .sum();

                return new WorkTimeStatResponse(
                    entry.getKey(),
                    count,
                    total,
                    count == 0 ? 0 : total / count
                );
            })
            .sorted(
                Comparator.comparing(
                    (WorkTimeStatResponse row) ->
                        row.workType()
                )
            )
            .toList();
    }

    @Transactional(readOnly = true)
    public DailyReportResponse daily(LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();

        List<WorkSession> sessions =
            sessionQueryRepository.search(
                from,
                to,
                null,
                null,
                null
            );

        Map<String, List<WorkSession>> byAssignmentAndMate =
            sessions.stream()
                .collect(
                    Collectors.groupingBy(
                        session ->
                            session.getWorkAssignment().getId()
                                + ":"
                                + session.getMate().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                    )
                );

        List<DailyWorkRow> works =
            byAssignmentAndMate.values().stream()
                .map(group -> {
                    WorkSession first = group.get(0);
                    WorkAssignment assignment =
                        first.getWorkAssignment();

                    long actualSeconds = group.stream()
                        .filter(session ->
                            session.getEndedAt() != null
                        )
                        .mapToLong(session ->
                            clippedSeconds(
                                session,
                                from,
                                to
                            )
                        )
                        .sum();

                    boolean uncertain = group.stream()
                        .anyMatch(session ->
                            session.getQualityStatus()
                                == WorkSessionQualityStatus.UNCERTAIN
                        );

                    String lastCompletedLocation =
                        progressRepository
                            .findFirstByWorkAssignmentIdAndMateIdAndReportedAtLessThanOrderByReportedAtDesc(
                                assignment.getId(),
                                first.getMate().getId(),
                                to
                            )
                            .map(progress ->
                                progress
                                    .getLastCompletedLocation()
                                    .getFullCode()
                            )
                            .orElse(null);

                    return new DailyWorkRow(
                        assignment.getId(),
                        first.getMate().getNickname(),
                        first
                            .getPdaUsageHistory()
                            .getPdaDevice()
                            .getDeviceNumber(),
                        assignment
                            .getWorkType()
                            .getName(),
                        assignment
                            .getAreaLocation()
                            .getFullCode(),
                        assignment
                            .getStartLocation()
                            .getFullCode(),
                        lastCompletedLocation,
                        actualSeconds,
                        uncertain
                            ? "UNCERTAIN"
                            : "NORMAL"
                    );
                })
                .toList();

        List<DailyPdaRow> pdaRows =
            pdaUsageRepository
                .findOverlapping(from, to)
                .stream()
                .map(this::toPdaRow)
                .toList();

        List<DailyIssueRow> issueRows =
            issueRepository
                .findAllByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                    from,
                    to
                )
                .stream()
                .map(this::toIssueRow)
                .toList();

        return new DailyReportResponse(
            date,
            works,
            pdaRows,
            issueRows
        );
    }

    @Transactional(readOnly = true)
    public List<LocalDate> recentShiftDates(
        int limit
    ) {
        int safeLimit =
            Math.max(
                1,
                Math.min(14, limit)
            );

        LocalDateTime to =
            LocalDateTime.now()
                .plusDays(1);

        LocalDateTime from =
            to.minusDays(31);

        return sessionQueryRepository
            .search(
                from,
                to,
                null,
                null,
                null
            )
            .stream()
            .map(this::sessionShiftDate)
            .distinct()
            .sorted(Comparator.reverseOrder())
            .limit(safeLimit)
            .toList();
    }

    @Transactional(readOnly = true)
    public ShiftReportResponse shift(
        LocalDate shiftDate
    ) {
        List<WorkSession> sessions =
            shiftSessions(shiftDate);

        List<SpecialIssue> issues =
            shiftIssues(shiftDate);

        Set<Long> sessionUsageIds =
            sessions.stream()
                .map(session ->
                    session
                        .getPdaUsageHistory()
                        .getId()
                )
                .collect(Collectors.toSet());

        LocalDateTime broadFrom =
            shiftDate.atStartOfDay();

        LocalDateTime broadTo =
            shiftDate
                .plusDays(2)
                .atStartOfDay();

        List<PdaUsageHistory> pdaUsages =
            pdaUsageRepository
                .findOverlapping(
                    broadFrom,
                    broadTo
                )
                .stream()
                .filter(usage ->
                    sessionUsageIds.contains(
                        usage.getId()
                    )
                        || scheduleResolver
                            .resolveShiftDate(
                                usage.getMate().getId(),
                                usage.getAssignedAt()
                            )
                            .equals(shiftDate)
                )
                .toList();

        Map<String, List<WorkSession>>
            byAssignmentAndMate =
                sessions.stream()
                    .collect(
                        Collectors.groupingBy(
                            session ->
                                session
                                    .getWorkAssignment()
                                    .getId()
                                    + ":"
                                    + session
                                        .getMate()
                                        .getId(),
                            LinkedHashMap::new,
                            Collectors.toList()
                        )
                    );

        List<DailyWorkRow> works =
            byAssignmentAndMate
                .values()
                .stream()
                .map(group ->
                    shiftWorkRow(group)
                )
                .toList();

        long actualWorkSeconds =
            sessions.stream()
                .filter(session ->
                    session.getEndedAt() != null
                )
                .mapToLong(session ->
                    Duration.between(
                        session.getStartedAt(),
                        session.getEndedAt()
                    ).getSeconds()
                )
                .sum();

        long openSessionCount =
            sessions.stream()
                .filter(session ->
                    session.getEndedAt() == null
                )
                .count();

        long uncertainSessionCount =
            sessions.stream()
                .filter(session ->
                    session.getQualityStatus()
                        == WorkSessionQualityStatus.UNCERTAIN
                )
                .count();

        long assignmentCount =
            sessions.stream()
                .map(session ->
                    session
                        .getWorkAssignment()
                        .getId()
                )
                .distinct()
                .count();

        long mateCount =
            sessions.stream()
                .map(session ->
                    session.getMate().getId()
                )
                .distinct()
                .count();

        long overnightSessionCount =
            sessions.stream()
                .filter(session ->
                    scheduleResolver.overnight(
                        session.getMate().getId(),
                        shiftDate
                    )
                )
                .count();

        ShiftReportSummary summary =
            new ShiftReportSummary(
                actualWorkSeconds,
                sessions.size(),
                openSessionCount,
                uncertainSessionCount,
                assignmentCount,
                mateCount,
                issues.size(),
                pdaUsages.size(),
                overnightSessionCount
            );

        ShiftComparisonResponse comparison =
            buildShiftComparison(
                shiftDate,
                summary
            );

        return new ShiftReportResponse(
            shiftDate,
            summary,
            comparison,
            works,
            pdaUsages.stream()
                .map(this::toPdaRow)
                .toList(),
            issues.stream()
                .map(this::toIssueRow)
                .toList()
        );
    }

    @Transactional(readOnly = true)
    public RangeReportResponse range(
        LocalDate fromDate,
        LocalDate toDate
    ) {
        validateRequiredRange(fromDate, toDate);

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to =
            toDate.plusDays(1).atStartOfDay();

        List<WorkSession> sessions =
            sessionQueryRepository
                .search(
                    from,
                    to,
                    null,
                    null,
                    null
                )
                .stream()
                .filter(session ->
                    session.getEndedAt() != null
                )
                .toList();

        List<SpecialIssue> issues =
            issueRepository
                .findAllByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                    from,
                    to
                );

        List<PdaUsageHistory> pdaUsages =
            pdaUsageRepository.findOverlapping(
                from,
                to
            );

        long normalSeconds = sessions.stream()
            .filter(session ->
                session.getQualityStatus()
                    == WorkSessionQualityStatus.NORMAL
            )
            .mapToLong(session ->
                clippedSeconds(session, from, to)
            )
            .sum();

        long uncertainSeconds = sessions.stream()
            .filter(session ->
                session.getQualityStatus()
                    == WorkSessionQualityStatus.UNCERTAIN
            )
            .mapToLong(session ->
                clippedSeconds(session, from, to)
            )
            .sum();

        long assignmentCount = sessions.stream()
            .map(session ->
                session.getWorkAssignment().getId()
            )
            .distinct()
            .count();

        long mateCount = sessions.stream()
            .map(session -> session.getMate().getId())
            .distinct()
            .count();

        RangeReportSummary summary =
            new RangeReportSummary(
                sessions.size(),
                assignmentCount,
                mateCount,
                normalSeconds,
                uncertainSeconds,
                issues.size(),
                pdaUsages.size()
            );

        List<MateWorkStatRow> mateRows =
            buildMateRows(
                sessions,
                from,
                to
            );

        List<AreaWorkStatRow> areaRows =
            buildAreaRows(
                sessions,
                from,
                to
            );

        List<DailyTrendRow> trendRows =
            buildTrendRows(
                fromDate,
                toDate,
                sessions,
                issues
            );

        return new RangeReportResponse(
            fromDate,
            toDate,
            summary,
            mateRows,
            areaRows,
            trendRows
        );
    }

    private List<MateWorkStatRow> buildMateRows(
        List<WorkSession> sessions,
        LocalDateTime from,
        LocalDateTime to
    ) {
        Map<Long, List<WorkSession>> grouped =
            sessions.stream()
                .collect(
                    Collectors.groupingBy(
                        session ->
                            session.getMate().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                    )
                );

        return grouped.values().stream()
            .map(group -> {
                WorkSession first = group.get(0);

                long normal = group.stream()
                    .filter(session ->
                        session.getQualityStatus()
                            == WorkSessionQualityStatus.NORMAL
                    )
                    .mapToLong(session ->
                        clippedSeconds(
                            session,
                            from,
                            to
                        )
                    )
                    .sum();

                long uncertain = group.stream()
                    .filter(session ->
                        session.getQualityStatus()
                            == WorkSessionQualityStatus.UNCERTAIN
                    )
                    .mapToLong(session ->
                        clippedSeconds(
                            session,
                            from,
                            to
                        )
                    )
                    .sum();

                long assignments = group.stream()
                    .map(session ->
                        session
                            .getWorkAssignment()
                            .getId()
                    )
                    .distinct()
                    .count();

                return new MateWorkStatRow(
                    first.getMate().getId(),
                    first.getMate().getEmployeeNo(),
                    first.getMate().getNickname(),
                    group.size(),
                    assignments,
                    normal,
                    uncertain
                );
            })
            .sorted(
                Comparator.comparing(
                    (MateWorkStatRow row) ->
                        row.nickname()
                )
            )
            .toList();
    }

    private List<AreaWorkStatRow> buildAreaRows(
        List<WorkSession> sessions,
        LocalDateTime from,
        LocalDateTime to
    ) {
        Map<String, List<WorkSession>> grouped =
            sessions.stream()
                .collect(
                    Collectors.groupingBy(
                        session ->
                            session
                                .getWorkAssignment()
                                .getAreaLocation()
                                .getId()
                                + ":"
                                + session
                                    .getWorkAssignment()
                                    .getWorkType()
                                    .getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                    )
                );

        return grouped.values().stream()
            .map(group -> {
                WorkSession first = group.get(0);
                WorkAssignment assignment =
                    first.getWorkAssignment();

                long normal = group.stream()
                    .filter(session ->
                        session.getQualityStatus()
                            == WorkSessionQualityStatus.NORMAL
                    )
                    .mapToLong(session ->
                        clippedSeconds(
                            session,
                            from,
                            to
                        )
                    )
                    .sum();

                long uncertain = group.stream()
                    .filter(session ->
                        session.getQualityStatus()
                            == WorkSessionQualityStatus.UNCERTAIN
                    )
                    .mapToLong(session ->
                        clippedSeconds(
                            session,
                            from,
                            to
                        )
                    )
                    .sum();

                long assignments = group.stream()
                    .map(session ->
                        session
                            .getWorkAssignment()
                            .getId()
                    )
                    .distinct()
                    .count();

                return new AreaWorkStatRow(
                    assignment
                        .getAreaLocation()
                        .getId(),
                    assignment
                        .getAreaLocation()
                        .getFullCode(),
                    assignment
                        .getWorkType()
                        .getId(),
                    assignment
                        .getWorkType()
                        .getName(),
                    group.size(),
                    assignments,
                    normal,
                    uncertain
                );
            })
            .sorted(
                Comparator.comparing(
                    (AreaWorkStatRow row) ->
                        row.area()
                ).thenComparing(
                    row -> row.workType()
                )
            )
            .toList();
    }

    private List<DailyTrendRow> buildTrendRows(
        LocalDate fromDate,
        LocalDate toDate,
        List<WorkSession> sessions,
        List<SpecialIssue> issues
    ) {
        List<DailyTrendRow> result =
            new ArrayList<>();

      for (
          LocalDate date = fromDate;
          !date.isAfter(toDate);
          date = date.plusDays(1)
      ) {
        final LocalDate currentDate = date;

        LocalDateTime dayStart =
            currentDate.atStartOfDay();

        LocalDateTime dayEnd =
            currentDate.plusDays(1)
                .atStartOfDay();

        long normal = sessions.stream()
            .filter(session ->
                session.getQualityStatus()
                    == WorkSessionQualityStatus.NORMAL
            )
            .mapToLong(session ->
                clippedSeconds(
                    session,
                    dayStart,
                    dayEnd
                )
            )
            .sum();

        long uncertain = sessions.stream()
            .filter(session ->
                session.getQualityStatus()
                    == WorkSessionQualityStatus.UNCERTAIN
            )
            .mapToLong(session ->
                clippedSeconds(
                    session,
                    dayStart,
                    dayEnd
                )
            )
            .sum();

        long issueCount = issues.stream()
            .filter(issue ->
                issue.getCreatedAt()
                    .toLocalDate()
                    .equals(currentDate)
            )
            .count();

        result.add(
            new DailyTrendRow(
                currentDate,
                normal,
                uncertain,
                issueCount
            )
        );
      }

        return result;
    }

    private List<WorkSession> shiftSessions(
        LocalDate shiftDate
    ) {
        LocalDateTime from =
            shiftDate.atStartOfDay();

        LocalDateTime to =
            shiftDate
                .plusDays(2)
                .atStartOfDay();

        return sessionQueryRepository
            .search(
                from,
                to,
                null,
                null,
                null
            )
            .stream()
            .filter(session ->
                sessionShiftDate(session)
                    .equals(shiftDate)
            )
            .toList();
    }

    private List<SpecialIssue> shiftIssues(
        LocalDate shiftDate
    ) {
        LocalDateTime from =
            shiftDate.atStartOfDay();

        LocalDateTime to =
            shiftDate
                .plusDays(2)
                .atStartOfDay();

        return issueRepository
            .findAllByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                from,
                to
            )
            .stream()
            .filter(issue ->
                scheduleResolver
                    .resolveShiftDate(
                        issue
                            .getAuthorMate()
                            .getId(),
                        issue.getCreatedAt()
                    )
                    .equals(shiftDate)
            )
            .toList();
    }

    private LocalDate sessionShiftDate(
        WorkSession session
    ) {
        if (session.getShiftDate() != null) {
            return session.getShiftDate();
        }

        return scheduleResolver.resolveShiftDate(
            session.getMate().getId(),
            session.getStartedAt()
        );
    }

    private DailyWorkRow shiftWorkRow(
        List<WorkSession> group
    ) {
        WorkSession first = group.get(0);

        WorkAssignment assignment =
            first.getWorkAssignment();

        long actualSeconds =
            group.stream()
                .filter(session ->
                    session.getEndedAt() != null
                )
                .mapToLong(session ->
                    Duration.between(
                        session.getStartedAt(),
                        session.getEndedAt()
                    ).getSeconds()
                )
                .sum();

        boolean uncertain =
            group.stream()
                .anyMatch(session ->
                    session.getQualityStatus()
                        == WorkSessionQualityStatus.UNCERTAIN
                );

        LocalDateTime progressCutoff =
            group.stream()
                .map(session ->
                    session.getEndedAt() == null
                        ? LocalDateTime.now()
                        : session.getEndedAt()
                )
                .max(LocalDateTime::compareTo)
                .orElse(first.getStartedAt())
                .plusNanos(1);

        String lastCompletedLocation =
            progressRepository
                .findFirstByWorkAssignmentIdAndMateIdAndReportedAtLessThanOrderByReportedAtDesc(
                    assignment.getId(),
                    first.getMate().getId(),
                    progressCutoff
                )
                .map(progress ->
                    progress
                        .getLastCompletedLocation()
                        .getFullCode()
                )
                .orElse(null);

        return new DailyWorkRow(
            assignment.getId(),
            first.getMate().getNickname(),
            first
                .getPdaUsageHistory()
                .getPdaDevice()
                .getDeviceNumber(),
            assignment
                .getWorkType()
                .getName(),
            assignment
                .getAreaLocation()
                .getFullCode(),
            assignment
                .getStartLocation()
                .getFullCode(),
            lastCompletedLocation,
            actualSeconds,
            uncertain
                ? "UNCERTAIN"
                : "NORMAL"
        );
    }

    private ShiftComparisonResponse buildShiftComparison(
        LocalDate shiftDate,
        ShiftReportSummary current
    ) {
        LocalDateTime searchFrom =
            shiftDate
                .minusDays(14)
                .atStartOfDay();

        LocalDateTime searchTo =
            shiftDate.atStartOfDay();

        Optional<LocalDate> previousShiftDate =
            sessionQueryRepository
                .search(
                    searchFrom,
                    searchTo,
                    null,
                    null,
                    null
                )
                .stream()
                .map(this::sessionShiftDate)
                .filter(date ->
                    date.isBefore(shiftDate)
                )
                .max(LocalDate::compareTo);

        if (previousShiftDate.isEmpty()) {
            return new ShiftComparisonResponse(
                null,
                0L,
                current.actualWorkSeconds(),
                0L,
                current.issueCount()
            );
        }

        LocalDate previous =
            previousShiftDate.get();

        List<WorkSession> previousSessions =
            shiftSessions(previous);

        long previousWorkSeconds =
            previousSessions.stream()
                .filter(session ->
                    session.getEndedAt() != null
                )
                .mapToLong(session ->
                    Duration.between(
                        session.getStartedAt(),
                        session.getEndedAt()
                    ).getSeconds()
                )
                .sum();

        long previousIssueCount =
            shiftIssues(previous).size();

        return new ShiftComparisonResponse(
            previous,
            previousWorkSeconds,
            current.actualWorkSeconds()
                - previousWorkSeconds,
            previousIssueCount,
            current.issueCount()
                - previousIssueCount
        );
    }

    private long clippedSeconds(
        WorkSession session,
        LocalDateTime from,
        LocalDateTime to
    ) {
        if (session.getEndedAt() == null) {
            return 0L;
        }

        LocalDateTime start =
            session.getStartedAt();
        LocalDateTime end =
            session.getEndedAt();

        if (from != null && start.isBefore(from)) {
            start = from;
        }

        if (to != null && end.isAfter(to)) {
            end = to;
        }

        if (!end.isAfter(start)) {
            return 0L;
        }

        return Duration.between(
            start,
            end
        ).getSeconds();
    }

    private DailyPdaRow toPdaRow(
        PdaUsageHistory usage
    ) {
        return new DailyPdaRow(
            usage
                .getPdaDevice()
                .getDeviceNumber(),
            usage
                .getMate()
                .getNickname(),
            usage.getAssignedAt(),
            usage.getReleasedAt(),
            usage.getReleaseReason() == null
                ? null
                : usage
                    .getReleaseReason()
                    .name()
        );
    }

    private DailyIssueRow toIssueRow(
        SpecialIssue issue
    ) {
        return new DailyIssueRow(
            issue.getId(),
            issue.getIssueType().getName(),
            issue.getAuthorMate().getNickname(),
            issue.getLocation() == null
                ? null
                : issue
                    .getLocation()
                    .getFullCode(),
            issue.getComment(),
            issue.getStatus().name(),
            issue.getCreatedAt()
        );
    }

    private void validateRange(
        LocalDate from,
        LocalDate to
    ) {
        if (from == null || to == null) {
            return;
        }

        validateRequiredRange(from, to);
    }

    private void validateRequiredRange(
        LocalDate from,
        LocalDate to
    ) {
        if (from == null || to == null) {
            throw new BusinessException(
                "REPORT_RANGE_REQUIRED",
                "조회 시작일과 종료일이 필요합니다."
            );
        }

        if (to.isBefore(from)) {
            throw new BusinessException(
                "REPORT_RANGE_INVALID",
                "종료일은 시작일보다 빠를 수 없습니다."
            );
        }

        long days =
            ChronoUnit.DAYS.between(
                from,
                to
            ) + 1;

        if (days > MAX_RANGE_DAYS) {
            throw new BusinessException(
                "REPORT_RANGE_TOO_LONG",
                "기간 통계는 최대 366일까지 조회할 수 있습니다."
            );
        }
    }
}
