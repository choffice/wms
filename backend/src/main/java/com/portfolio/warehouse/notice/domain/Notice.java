package com.portfolio.warehouse.notice.domain;

import com.portfolio.warehouse.auth.domain.UserAccount;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notice")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private boolean visible = true;

    @Column(nullable = false)
    private boolean important;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_account_id", nullable = false)
    private UserAccount createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Notice() {}

    public Notice(String content, boolean visible, boolean important, int displayOrder, UserAccount createdBy) {
        this.content = content;
        this.visible = visible;
        this.important = important;
        this.displayOrder = displayOrder;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() { return id; }
    public String getContent() { return content; }
    public boolean isVisible() { return visible; }
    public boolean isImportant() { return important; }
    public int getDisplayOrder() { return displayOrder; }
    public UserAccount getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }

    public void update(String content, boolean visible, boolean important) {
        this.content = content;
        this.visible = visible;
        this.important = important;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeOrder(int order) {
        this.displayOrder = order;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }
}
