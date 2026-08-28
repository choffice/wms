package com.portfolio.warehouse.work.domain;

import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.pda.domain.PdaUsageHistory;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "work_session",
    indexes = {
        @Index(
            name = "idx_work_session_shift_date",
            columnList = "shift_date"
        ),
        @Index(
            name = "idx_work_session_mate_open",
            columnList = "mate_id, ended_at"
        )
    }
)
public class WorkSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_assignment_id", nullable = false)
    private WorkAssignment workAssignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mate_id", nullable = false)
    private Mate mate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pda_usage_history_id", nullable = false)
    private PdaUsageHistory pdaUsageHistory;

    @Column(name = "shift_date")
    private LocalDate shiftDate;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", length = 30)
    private WorkSessionEndReason endReason;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_status", nullable = false, length = 20)
    private WorkSessionQualityStatus qualityStatus = WorkSessionQualityStatus.NORMAL;

    protected WorkSession() {
    }

    public WorkSession(
        WorkAssignment workAssignment,
        Mate mate,
        PdaUsageHistory pdaUsageHistory,
        LocalDate shiftDate,
        LocalDateTime startedAt
    ) {
        this.workAssignment = workAssignment;
        this.mate = mate;
        this.pdaUsageHistory = pdaUsageHistory;
        this.shiftDate = shiftDate;
        this.startedAt = startedAt;
        this.lastHeartbeatAt = startedAt;
    }

    public Long getId() { return id; }
    public WorkAssignment getWorkAssignment() { return workAssignment; }
    public Mate getMate() { return mate; }
    public PdaUsageHistory getPdaUsageHistory() { return pdaUsageHistory; }
    public LocalDate getShiftDate() { return shiftDate; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public WorkSessionEndReason getEndReason() { return endReason; }
    public LocalDateTime getLastHeartbeatAt() { return lastHeartbeatAt; }
    public WorkSessionQualityStatus getQualityStatus() { return qualityStatus; }

    public boolean isOpen() {
        return endedAt == null;
    }

    public void heartbeat(LocalDateTime at) {
        if (!isOpen()) {
            throw new IllegalStateException("종료된 작업 세션입니다.");
        }
        this.lastHeartbeatAt = at;
    }

    public void close(LocalDateTime endedAt, WorkSessionEndReason reason) {
        if (!isOpen()) {
            throw new IllegalStateException("이미 종료된 작업 세션입니다.");
        }
        this.endedAt = endedAt;
        this.endReason = reason;
    }

    public void markUncertain() {
        this.qualityStatus = WorkSessionQualityStatus.UNCERTAIN;
    }

    @Transient
    public Duration getDuration() {
        if (endedAt == null) {
            return Duration.ZERO;
        }
        return Duration.between(startedAt, endedAt);
    }
}
