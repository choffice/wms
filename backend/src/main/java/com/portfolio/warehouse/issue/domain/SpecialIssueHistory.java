package com.portfolio.warehouse.issue.domain;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.mate.domain.Mate;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "special_issue_history")
public class SpecialIssueHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "special_issue_id", nullable = false)
    private SpecialIssue specialIssue;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private SpecialIssueHistoryAction actionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_responsible_mate_id")
    private Mate fromResponsibleMate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_responsible_mate_id")
    private Mate toResponsibleMate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_account_id", nullable = false)
    private UserAccount actorAccount;

    @Column(length = 300)
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    protected SpecialIssueHistory() {}

    public SpecialIssueHistory(
        SpecialIssue specialIssue,
        SpecialIssueHistoryAction actionType,
        Mate fromResponsibleMate,
        Mate toResponsibleMate,
        UserAccount actorAccount,
        String reason
    ) {
        this.specialIssue = specialIssue;
        this.actionType = actionType;
        this.fromResponsibleMate = fromResponsibleMate;
        this.toResponsibleMate = toResponsibleMate;
        this.actorAccount = actorAccount;
        this.reason = reason;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public SpecialIssue getSpecialIssue() { return specialIssue; }
    public SpecialIssueHistoryAction getActionType() { return actionType; }
    public Mate getFromResponsibleMate() { return fromResponsibleMate; }
    public Mate getToResponsibleMate() { return toResponsibleMate; }
    public UserAccount getActorAccount() { return actorAccount; }
    public String getReason() { return reason; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
