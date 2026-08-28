package com.portfolio.warehouse.issue.domain;

import jakarta.persistence.*;

@Entity
@Table(
    name = "issue_type",
    uniqueConstraints = @UniqueConstraint(name = "uk_issue_type_name", columnNames = "name")
)
public class IssueType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "require_location", nullable = false)
    private boolean requireLocation;

    @Column(name = "require_product_code", nullable = false)
    private boolean requireProductCode;

    @Column(name = "require_quantity", nullable = false)
    private boolean requireQuantity;

    @Column(nullable = false)
    private boolean active = true;

    protected IssueType() {}

    public IssueType(
        String name,
        boolean requireLocation,
        boolean requireProductCode,
        boolean requireQuantity
    ) {
        this.name = name;
        this.requireLocation = requireLocation;
        this.requireProductCode = requireProductCode;
        this.requireQuantity = requireQuantity;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public boolean isRequireLocation() { return requireLocation; }
    public boolean isRequireProductCode() { return requireProductCode; }
    public boolean isRequireQuantity() { return requireQuantity; }
    public boolean isActive() { return active; }

    public void update(
        String name,
        boolean requireLocation,
        boolean requireProductCode,
        boolean requireQuantity
    ) {
        this.name = name;
        this.requireLocation = requireLocation;
        this.requireProductCode = requireProductCode;
        this.requireQuantity = requireQuantity;
    }

    public void deactivate() { this.active = false; }
}
