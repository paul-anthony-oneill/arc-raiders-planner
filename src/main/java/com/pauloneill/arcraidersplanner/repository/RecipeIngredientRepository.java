package com.pauloneill.arcraidersplanner.repository;

import com.pauloneill.arcraidersplanner.model.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
    List<RecipeIngredient> findByRecipeId(Long recipeId);
    void deleteAllByRecipeId(Long recipeId);
}
