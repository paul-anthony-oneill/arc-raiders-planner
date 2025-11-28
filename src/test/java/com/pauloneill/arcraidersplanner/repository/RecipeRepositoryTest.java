package com.pauloneill.arcraidersplanner.repository;

import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.model.Recipe;
import com.pauloneill.arcraidersplanner.model.RecipeIngredient;
import com.pauloneill.arcraidersplanner.model.RecipeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.TestPropertySource;

@DataJpaTest
public class RecipeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private ItemRepository itemRepository;

    private Item testItem;

    @BeforeEach
    void setUp() {
        // Ensure an item exists to link with recipe ingredients
        testItem = new Item();
        testItem.setName("Test Item");
        testItem.setDescription("A common test item");
        testItem.setRarity("Common");
        testItem.setItemType("Material");
        testItem.setValue(10);
        testItem.setWeight(0.1);
        testItem.setStackSize(100);
        // Do not set LootType here, as it may require another entity to be persisted.
        // For repository tests, we focus on the current entity's relationships.
        entityManager.persist(testItem);
        entityManager.flush();
    }

    @Test
    void testSaveAndRetrieveRecipe() {
        // Create RecipeIngredient
        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.setItem(testItem);
        ingredient.setQuantity(5);

        // Create Recipe
        Recipe recipe = new Recipe();
        recipe.setName("Test Recipe");
        recipe.setDescription("A recipe for testing");
        recipe.setType(RecipeType.CRAFTING);
        recipe.addIngredient(ingredient);

        // Save Recipe
        Recipe savedRecipe = recipeRepository.save(recipe);
        entityManager.flush();
        entityManager.clear();

        // Retrieve Recipe
        Optional<Recipe> foundRecipe = recipeRepository.findById(savedRecipe.getId());

        // Assertions
        assertThat(foundRecipe).isPresent();
        assertThat(foundRecipe.get().getName()).isEqualTo("Test Recipe");
        assertThat(foundRecipe.get().getType()).isEqualTo(RecipeType.CRAFTING);
        assertThat(foundRecipe.get().getIngredients()).hasSize(1);
        assertThat(foundRecipe.get().getIngredients().iterator().next().getItem().getName()).isEqualTo(testItem.getName());
        assertThat(foundRecipe.get().getIngredients().iterator().next().getQuantity()).isEqualTo(5);
    }

    @Test
    void testFindByName() {
        // Create RecipeIngredient
        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.setItem(testItem);
        ingredient.setQuantity(2);

        // Create Recipe
        Recipe recipe = new Recipe();
        recipe.setName("Unique Recipe Name");
        recipe.setDescription("Another recipe for testing unique name lookup");
        recipe.setType(RecipeType.WORKBENCH_UPGRADE);
        recipe.addIngredient(ingredient);

        recipeRepository.save(recipe);
        entityManager.flush();
        entityManager.clear();

        // Add a method to RecipeRepository to find by name
        // This test will initially fail because findByName doesn't exist
        // Update: I need to define findByName in RecipeRepository first for this test to compile
        // For TDD, I'd define the method in the interface, run the test (will fail due to no implementation, which is okay for TDD of interface methods)
        // But for repository, Spring Data JPA implements it, so it should compile if method signature is valid.
        // Let's add it to the interface before running the test.

        Optional<Recipe> foundRecipe = recipeRepository.findByName("Unique Recipe Name");

        assertThat(foundRecipe).isPresent();
        assertThat(foundRecipe.get().getName()).isEqualTo("Unique Recipe Name");
        assertThat(foundRecipe.get().getType()).isEqualTo(RecipeType.WORKBENCH_UPGRADE);
        assertThat(foundRecipe.get().getIngredients()).hasSize(1);
        assertThat(foundRecipe.get().getIngredients().iterator().next().getItem().getName()).isEqualTo(testItem.getName());
        assertThat(foundRecipe.get().getIngredients().iterator().next().getQuantity()).isEqualTo(2);
    }
}
