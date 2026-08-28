package com.portfolio.warehouse.location.api.dto;

import com.portfolio.warehouse.location.domain.LocationFoodType;
import com.portfolio.warehouse.location.domain.NonFoodCategory;
import jakarta.validation.constraints.*;
import java.util.Set;

public record LocationAreaRangeRequest(
    @NotBlank
    @Pattern(regexp = "[A-Za-z]+", message = "알파벳 구역값만 입력해주세요.")
    String alphabet,

    @NotNull @Min(0) Integer startNumber,
    @NotNull @Min(0) Integer endNumber,

    @Min(1) @Max(6)
    Integer width,

    @Min(0)
    Integer floor,

    LocationFoodType foodType,
    Set<NonFoodCategory> nonFoodCategories
) {}
