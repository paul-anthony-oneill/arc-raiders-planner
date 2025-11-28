package com.pauloneill.arcraidersplanner.dto;

import com.pauloneill.arcraidersplanner.model.RecipeType;
import java.util.List;

public record RecipeDto(
    Long id,
    String name,
    String description,
    RecipeType type,
    List<RecipeIngredientDto> ingredients
) {}
