package com.portfolio.warehouse.work.domain;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.location.domain.Location;
import com.portfolio.warehouse.mate.domain.Mate;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_progress")
public class WorkProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_assignment_id", nullable = false)
    private WorkAssignment workAssignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mate_id", nullable = false)
    private Mate mate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_account_id")
    private UserAccount reportedByAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "last_completed_location_id", nullable = false)
    private Location lastCompletedLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_location_id")
    private Location previousLocation;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    @Column(nullable = false)
    private boolean correction;

    @Column(length = 300)
    private String reason;

    protected WorkProgress() {}

    public WorkProgress(
        WorkAssignment workAssignment,
        Mate mate,
        UserAccount reportedByAccount,
        Location lastCompletedLocation,
        Location previousLocation,
        boolean correction,
        String reason
    ) {
        this.workAssignment = workAssignment;
        this.mate = mate;
        this.reportedByAccount = reportedByAccount;
        this.lastCompletedLocation = lastCompletedLocation;
        this.previousLocation = previousLocation;
        this.correction = correction;
        this.reason = reason;
        this.reportedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public WorkAssignment getWorkAssignment() { return workAssignment; }
    public Mate getMate() { return mate; }
    public UserAccount getReportedByAccount() { return reportedByAccount; }
    public Location getLastCompletedLocation() { return lastCompletedLocation; }
    public Location getPreviousLocation() { return previousLocation; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public boolean isCorrection() { return correction; }
    public String getReason() { return reason; }
}
