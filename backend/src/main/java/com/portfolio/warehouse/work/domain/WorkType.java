package com.portfolio.warehouse.work.domain;

import jakarta.persistence.*;

@Entity
@Table(
    name = "work_type",
    uniqueConstraints = @UniqueConstraint(name = "uk_work_type_name", columnNames = "name")
)
public class WorkType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 300)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    protected WorkType() {
    }

    public WorkType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void deactivate() {
        this.active = false;
    }
}
