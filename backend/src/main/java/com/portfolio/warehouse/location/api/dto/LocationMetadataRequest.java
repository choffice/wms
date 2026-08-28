package com.portfolio.warehouse.location.api.dto;

import com.portfolio.warehouse.location.domain.LocationFoodType;
import com.portfolio.warehouse.location.domain.NonFoodCategory;
import jakarta.validation.constraints.Min;
import java.util.Set;

public record LocationMetadataRequest(
    @Min(0) Integer floor,
    LocationFoodType foodType,
    Set<NonFoodCategory> nonFoodCategories
) {}
