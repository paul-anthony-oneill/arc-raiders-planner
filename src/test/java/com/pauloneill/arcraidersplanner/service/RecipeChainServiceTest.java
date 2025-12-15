package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.RecipeChainDto;
import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.model.Recipe;
import com.pauloneill.arcraidersplanner.model.RecipeIngredient;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import com.pauloneill.arcraidersplanner.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for Recipe Chain functionality in ItemServiceImpl
 * WHY: Validates prerequisite detection for crafting upgrades (e.g., Anvil III → Anvil IV)
 */
@ExtendWith(MockitoExtension.class)
class RecipeChainServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    private Item anvilIV;
    private Item anvilIII;
    private Item steelPlate;
    private Item mechanicalParts;
    private Recipe anvilIVRecipe;
    private Recipe anvilIIIRecipe;

    @BeforeEach
    void setUp() {
        // Anvil IV (target item)
        anvilIV = new Item();
        anvilIV.setId(1L);
        anvilIV.setName("Anvil IV");
        anvilIV.setMetaforgeId("anvil-iv");
        anvilIV.setItemType("Upgrade");

        // Anvil III (prerequisite)
        anvilIII = new Item();
        anvilIII.setId(2L);
        anvilIII.setName("Anvil III");
        anvilIII.setMetaforgeId("anvil-iii");
        anvilIII.setItemType("Upgrade");

        // Steel Plate (material)
        steelPlate = new Item();
        steelPlate.setId(3L);
        steelPlate.setName("Steel Plate");
        steelPlate.setMetaforgeId("steel-plate");
        steelPlate.setItemType("Material");

        // Mechanical Parts (material)
        mechanicalParts = new Item();
        mechanicalParts.setId(4L);
        mechanicalParts.setName("Mechanical Parts");
        mechanicalParts.setMetaforgeId("mechanical-parts");
        mechanicalParts.setItemType("Material");

        // Recipe for Anvil IV: Anvil III + Steel Plate + Mechanical Parts
        anvilIVRecipe = new Recipe();
        anvilIVRecipe.setId(1L);
        anvilIVRecipe.setMetaforgeItemId("anvil-iv");

        RecipeIngredient ingredient1 = new RecipeIngredient();
        ingredient1.setItem(anvilIII);
        ingredient1.setQuantity(1);

        RecipeIngredient ingredient2 = new RecipeIngredient();
        ingredient2.setItem(steelPlate);
        ingredient2.setQuantity(5);

        RecipeIngredient ingredient3 = new RecipeIngredient();
        ingredient3.setItem(mechanicalParts);
        ingredient3.setQuantity(3);

        anvilIVRecipe.setIngredients(new HashSet<>(Arrays.asList(ingredient1, ingredient2, ingredient3)));

        // Recipe for Anvil III (the prerequisite also has a recipe)
        anvilIIIRecipe = new Recipe();
        anvilIIIRecipe.setId(2L);
        anvilIIIRecipe.setMetaforgeItemId("anvil-iii");
    }

    @Test
    void shouldDetectPrerequisiteWhenSameItemTypeAndHasRecipe() {
        // Given
        when(itemRepository.findById(1L)).thenReturn(Optional.of(anvilIV));
        when(recipeRepository.findByMetaforgeItemId(anyString())).thenAnswer(invocation -> {
            String metaforgeId = invocation.getArgument(0);
            if ("anvil-iv".equals(metaforgeId)) return Optional.of(anvilIVRecipe);
            if ("anvil-iii".equals(metaforgeId)) return Optional.of(anvilIIIRecipe);
            return Optional.empty();
        });

        // When
        RecipeChainDto result = itemService.getRecipeChain(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItemName()).isEqualTo("Anvil IV");
        assertThat(result.getIngredients()).hasSize(3);

        // Find the Anvil III ingredient
        RecipeChainDto.RecipeIngredientChainDto prerequisite = result.getIngredients().stream()
                .filter(i -> i.getItemName().equals("Anvil III"))
                .findFirst()
                .orElseThrow();

        assertThat(prerequisite.isPrerequisite()).isTrue();
        assertThat(prerequisite.getRecipeId()).isEqualTo(2L);
        assertThat(prerequisite.getQuantity()).isEqualTo(1);
    }

    @Test
    void shouldNotMarkMaterialsAsPrerequisites() {
        // Given
        when(itemRepository.findById(1L)).thenReturn(Optional.of(anvilIV));
        when(recipeRepository.findByMetaforgeItemId(anyString())).thenAnswer(invocation -> {
            String metaforgeId = invocation.getArgument(0);
            if ("anvil-iv".equals(metaforgeId)) return Optional.of(anvilIVRecipe);
            if ("anvil-iii".equals(metaforgeId)) return Optional.of(anvilIIIRecipe);
            return Optional.empty();
        });

        // When
        RecipeChainDto result = itemService.getRecipeChain(1L);

        // Then
        assertThat(result.getIngredients()).hasSize(3);

        // Materials should not be marked as prerequisites
        RecipeChainDto.RecipeIngredientChainDto steelPlateIng = result.getIngredients().stream()
                .filter(i -> i.getItemName().equals("Steel Plate"))
                .findFirst()
                .orElseThrow();

        assertThat(steelPlateIng.isPrerequisite()).isFalse();
        assertThat(steelPlateIng.getRecipeId()).isNull();
    }

    @Test
    void shouldReturnNullWhenItemHasNoMetaforgeId() {
        // Given
        Item itemWithoutMetaforgeId = new Item();
        itemWithoutMetaforgeId.setId(5L);
        itemWithoutMetaforgeId.setName("Custom Item");
        itemWithoutMetaforgeId.setMetaforgeId(null);

        when(itemRepository.findById(5L)).thenReturn(Optional.of(itemWithoutMetaforgeId));

        // When
        RecipeChainDto result = itemService.getRecipeChain(5L);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenItemHasNoRecipe() {
        // Given
        when(itemRepository.findById(1L)).thenReturn(Optional.of(anvilIV));
        when(recipeRepository.findByMetaforgeItemId("anvil-iv")).thenReturn(Optional.empty());

        // When
        RecipeChainDto result = itemService.getRecipeChain(1L);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldThrowExceptionWhenItemNotFound() {
        // Given
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> itemService.getRecipeChain(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Item not found")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnCraftableMetaforgeIds() {
        // Given
        List<String> craftableIds = Arrays.asList("anvil-iv", "anvil-iii", "steel-blade");
        when(recipeRepository.findAllMetaforgeItemIdsWithRecipes()).thenReturn(craftableIds);

        // When
        List<String> result = itemService.getCraftableMetaforgeIds();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyInAnyOrder("anvil-iv", "anvil-iii", "steel-blade");
    }

    @Test
    void shouldHandleComplexPrerequisiteChains() {
        // Given - Workbench IV requires Workbench III (which also has a recipe)
        Item workbenchIV = new Item();
        workbenchIV.setId(10L);
        workbenchIV.setName("Workbench IV");
        workbenchIV.setMetaforgeId("workbench-iv");
        workbenchIV.setItemType("Workbench");

        Item workbenchIII = new Item();
        workbenchIII.setId(11L);
        workbenchIII.setName("Workbench III");
        workbenchIII.setMetaforgeId("workbench-iii");
        workbenchIII.setItemType("Workbench");

        Recipe workbenchIVRecipe = new Recipe();
        workbenchIVRecipe.setId(10L);
        workbenchIVRecipe.setMetaforgeItemId("workbench-iv");

        RecipeIngredient prerequisiteIngredient = new RecipeIngredient();
        prerequisiteIngredient.setItem(workbenchIII);
        prerequisiteIngredient.setQuantity(1);

        workbenchIVRecipe.setIngredients(new HashSet<>(Arrays.asList(prerequisiteIngredient)));

        Recipe workbenchIIIRecipe = new Recipe();
        workbenchIIIRecipe.setId(11L);
        workbenchIIIRecipe.setMetaforgeItemId("workbench-iii");

        when(itemRepository.findById(10L)).thenReturn(Optional.of(workbenchIV));
        when(recipeRepository.findByMetaforgeItemId(anyString())).thenAnswer(invocation -> {
            String metaforgeId = invocation.getArgument(0);
            if ("workbench-iv".equals(metaforgeId)) return Optional.of(workbenchIVRecipe);
            if ("workbench-iii".equals(metaforgeId)) return Optional.of(workbenchIIIRecipe);
            return Optional.empty();
        });

        // When
        RecipeChainDto result = itemService.getRecipeChain(10L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIngredients()).hasSize(1);

        RecipeChainDto.RecipeIngredientChainDto prerequisite = result.getIngredients().get(0);
        assertThat(prerequisite.isPrerequisite()).isTrue();
        assertThat(prerequisite.getItemName()).isEqualTo("Workbench III");
        assertThat(prerequisite.getRecipeId()).isEqualTo(11L);
    }
}
