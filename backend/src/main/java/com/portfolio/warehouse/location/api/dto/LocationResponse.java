package com.portfolio.warehouse.location.api.dto;

import com.portfolio.warehouse.location.domain.*;
import java.util.Set;

public record LocationResponse(
    Long id,
    Long parentId,
    String segment,
    String fullCode,
    int depth,
    Integer floor,
    LocationFoodType foodType,
    Set<NonFoodCategory> nonFoodCategories,
    boolean active
) {
    public static LocationResponse from(Location entity) {
        return new LocationResponse(
            entity.getId(),
            entity.getParent() == null ? null : entity.getParent().getId(),
            entity.getSegment(),
            entity.getFullCode(),
            entity.getDepth(),
            entity.getFloor(),
            entity.getFoodType(),
            entity.getNonFoodCategories(),
            entity.isActive()
        );
    }
}
