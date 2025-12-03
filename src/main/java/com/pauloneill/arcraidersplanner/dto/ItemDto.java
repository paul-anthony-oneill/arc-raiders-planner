package com.pauloneill.arcraidersplanner.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class ItemDto {
    private Long id;
    private String name;
    private String description;
    private String rarity;
    private String itemType;
    private String lootType; // Just the name of the loot type
    private String iconUrl;
    private Integer value;
    private Double weight;
    private Integer stackSize;

    // Detail panel fields for unified tactical planner UI
    private Set<String> droppedBy;           // Enemy IDs that drop this item
    private List<RecipeDto> usedInRecipes;   // Recipes that use this item as ingredient
    private RecipeDto craftingRecipe;        // Recipe to craft this item (if craftable)
}
