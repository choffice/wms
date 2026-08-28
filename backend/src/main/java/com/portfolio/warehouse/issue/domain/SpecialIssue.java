package com.portfolio.warehouse.issue.domain;

import com.portfolio.warehouse.location.domain.Location;
import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.work.domain.WorkAssignment;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "special_issue")
public class SpecialIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_type_id", nullable = false)
    private IssueType issueType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_mate_id", nullable = false)
    private Mate authorMate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_mate_id")
    private Mate responsibleMate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_assignment_id")
    private WorkAssignment workAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "product_code", length = 80)
    private String productCode;

    private Integer quantity;

    @Column(name = "actual_stock")
    private Integer actualStock;

    @Column(name = "mms_stock")
    private Integer mmsStock;

    @Column(name = "expiry_stock")
    private Integer expiryStock;

    @Column(name = "no_stock", nullable = false)
    private boolean noStock;

    @Column(nullable = false, length = 1200)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueStatus status = IssueStatus.UNCONFIRMED;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected SpecialIssue() {}

    public SpecialIssue(
        IssueType issueType,
        Mate authorMate,
        Mate responsibleMate,
        WorkAssignment workAssignment,
        Location location,
        String productCode,
        Integer quantity,
        Integer actualStock,
        Integer mmsStock,
        Integer expiryStock,
        boolean noStock,
        String comment
    ) {
        this.issueType = issueType;
        this.authorMate = authorMate;
        this.responsibleMate = responsibleMate;
        this.workAssignment = workAssignment;
        this.location = location;
        this.productCode = productCode;
        this.quantity = quantity;
        this.actualStock = actualStock;
        this.mmsStock = mmsStock;
        this.expiryStock = expiryStock;
        this.noStock = noStock;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() { return id; }
    public IssueType getIssueType() { return issueType; }
    public Mate getAuthorMate() { return authorMate; }
    public Mate getResponsibleMate() { return responsibleMate; }
    public WorkAssignment getWorkAssignment() { return workAssignment; }
    public Location getLocation() { return location; }
    public String getProductCode() { return productCode; }
    public Integer getQuantity() { return quantity; }
    public Integer getActualStock() { return actualStock; }
    public Integer getMmsStock() { return mmsStock; }
    public Integer getExpiryStock() { return expiryStock; }
    public boolean isNoStock() { return noStock; }
    public String getComment() { return comment; }
    public IssueStatus getStatus() { return status; }
    public long getViewCount() { return viewCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }

    public void assignResponsible(Mate mate) {
        this.responsibleMate = mate;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementViewCount() { this.viewCount++; }

    public void confirm() {
        this.status = IssueStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.updatedAt = this.confirmedAt;
    }

    public void resolve() {
        this.status = IssueStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.updatedAt = this.resolvedAt;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }
}
