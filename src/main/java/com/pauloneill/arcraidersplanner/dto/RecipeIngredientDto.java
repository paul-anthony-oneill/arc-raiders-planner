package com.pauloneill.arcraidersplanner.dto;

public record RecipeIngredientDto(
    Long itemId,
    String itemName,
    Integer quantity
) {}
