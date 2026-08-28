package com.portfolio.warehouse.location.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
    name = "location",
    uniqueConstraints = @UniqueConstraint(name = "uk_location_full_code", columnNames = "full_code")
)
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Location parent;

    @Column(nullable = false, length = 20)
    private String segment;

    @Column(name = "full_code", nullable = false, length = 120)
    private String fullCode;

    @Column(nullable = false)
    private int depth;

    @Column
    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_type", nullable = false, length = 20)
    private LocationFoodType foodType = LocationFoodType.NON_FOOD;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "location_non_food_category",
        joinColumns = @JoinColumn(name = "location_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private Set<NonFoodCategory> nonFoodCategories = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Location() {}

    public Location(Location parent, String segment, String fullCode, int depth) {
        this.parent = parent;
        this.segment = segment;
        this.fullCode = fullCode;
        this.depth = depth;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Location getParent() { return parent; }
    public String getSegment() { return segment; }
    public String getFullCode() { return fullCode; }
    public int getDepth() { return depth; }
    public Integer getFloor() { return floor; }
    public LocationFoodType getFoodType() { return foodType; }
    public Set<NonFoodCategory> getNonFoodCategories() { return Set.copyOf(nonFoodCategories); }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void updateMetadata(
        Integer floor,
        LocationFoodType foodType,
        Set<NonFoodCategory> categories
    ) {
        this.floor = floor;
        this.foodType = foodType == null ? LocationFoodType.NON_FOOD : foodType;
        this.nonFoodCategories.clear();

        if (this.foodType == LocationFoodType.NON_FOOD && categories != null) {
            this.nonFoodCategories.addAll(categories);
        }
    }

    public void deactivate() { this.active = false; }
    public void reactivate() { this.active = true; }
}
