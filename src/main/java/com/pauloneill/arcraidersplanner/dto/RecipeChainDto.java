package com.pauloneill.arcraidersplanner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipeChainDto {
    private Long itemId;
    private String itemName;
    private Long recipeId;
    private List<RecipeIngredientChainDto> ingredients;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RecipeIngredientChainDto {
        private Long itemId;
        private String itemName;
        private Integer quantity;
        private boolean isPrerequisite; // True if this is likely the lower-tier item (e.g. Anvil III for Anvil IV)
        private Long recipeId; // If this ingredient has its own recipe
    }
}
