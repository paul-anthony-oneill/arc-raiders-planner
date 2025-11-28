package com.pauloneill.arcraidersplanner.repository;

import com.pauloneill.arcraidersplanner.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
