package com.pauloneill.arcraidersplanner.repository;

import com.pauloneill.arcraidersplanner.model.Recipe;
import com.pauloneill.arcraidersplanner.model.RecipeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    Optional<Recipe> findByName(String name);

    /**
     * Finds a recipe by its Metaforge API item ID.
     * WHY: Enables idempotent sync - update existing recipe rather than create duplicate
     *
     * @param metaforgeItemId The Metaforge API item.id
     * @return Optional containing recipe if found
     */
    Optional<Recipe> findByMetaforgeItemId(String metaforgeItemId);

    /**
     * Finds all recipes of a specific type.
     * WHY: Enables filtering workbench upgrades from crafting recipes for API endpoints
     *
     * @param type The recipe type (CRAFTING or WORKBENCH_UPGRADE)
     * @return List of recipes matching the type
     */
    List<Recipe> findByType(RecipeType type);

    /**
     * Finds all recipes that use a specific item as an ingredient.
     * WHY: Required for "Used In Recipes" section of item detail view in tactical planner UI
     *
     * @param itemId The item ID to search for in recipe ingredients
     * @return List of recipes that contain this item as an ingredient
     */
    @Query("SELECT DISTINCT r FROM Recipe r JOIN r.ingredients i WHERE i.item.id = :itemId")
    List<Recipe> findRecipesUsingItem(@Param("itemId") Long itemId);
}
