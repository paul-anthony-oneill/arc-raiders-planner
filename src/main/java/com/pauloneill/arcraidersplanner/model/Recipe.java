package com.pauloneill.arcraidersplanner.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecipeType type;

    // Links to Metaforge API item.id for idempotent sync
    // WHY: Prevents duplicate recipes when re-syncing from API
    @Column(name = "metaforge_item_id", unique = true)
    private String metaforgeItemId;

    // Indicates if this is a recycling recipe (vs crafting)
    // WHY: Reserved for Phase 3 - distinguishes recycle_components from components
    @Column(name = "is_recyclable")
    private Boolean isRecyclable = false;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<RecipeIngredient> ingredients = new HashSet<>();

    public void addIngredient(RecipeIngredient ingredient) {
        ingredients.add(ingredient);
        ingredient.setRecipe(this);
    }

    public void removeIngredient(RecipeIngredient ingredient) {
        ingredients.remove(ingredient);
        ingredient.setRecipe(null);
    }
}
