package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.ItemDto;
import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.model.Recipe;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import com.pauloneill.arcraidersplanner.repository.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final RecipeRepository recipeRepository;
    private final DtoMapper dtoMapper;

    public ItemServiceImpl(ItemRepository itemRepository, RecipeRepository recipeRepository, DtoMapper dtoMapper) {
        this.itemRepository = itemRepository;
        this.recipeRepository = recipeRepository;
        this.dtoMapper = dtoMapper;
    }

    @Override
    public List<Item> searchItems(String query) {
        return itemRepository.findByNameContainingIgnoreCase(query);
    }

    @Override
    public List<Item> getAllItems() {
        return itemRepository.findTop50ByOrderByNameAsc();
    }

    /**
     * Get item with full crafting/usage context.
     * WHY: Provides all data needed for center panel detail view in tactical planner UI
     */
    @Override
    public ItemDto getItemWithContext(Long itemId) {
        // Fetch item
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Item not found with ID: " + itemId));

        // Map to DTO
        ItemDto dto = dtoMapper.toDto(item);

        // Add droppedBy (already in Item entity)
        dto.setDroppedBy(item.getDroppedBy());

        // Find crafting recipe for this item (recipe that produces this item)
        if (item.getMetaforgeId() != null) {
            recipeRepository.findByMetaforgeItemId(item.getMetaforgeId())
                .ifPresent(recipe -> dto.setCraftingRecipe(dtoMapper.toDto(recipe)));
        }

        // Find recipes that use this item as ingredient
        List<Recipe> usageRecipes = recipeRepository.findRecipesUsingItem(itemId);
        dto.setUsedInRecipes(dtoMapper.toRecipeDtos(usageRecipes));

        return dto;
    }
    @Override
    public List<String> getCraftableMetaforgeIds() {
        return recipeRepository.findAllMetaforgeItemIdsWithRecipes();
    }
    @Override
    public com.pauloneill.arcraidersplanner.dto.RecipeChainDto getRecipeChain(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Item not found"));

        if (item.getMetaforgeId() == null) return null;

        Recipe recipe = recipeRepository.findByMetaforgeItemId(item.getMetaforgeId()).orElse(null);
        if (recipe == null) return null;

        List<com.pauloneill.arcraidersplanner.dto.RecipeChainDto.RecipeIngredientChainDto> ingredientDtos = recipe.getIngredients().stream()
                .map(ing -> {
                    Item ingItem = ing.getItem();
                    // Check if this ingredient has a recipe
                    Long ingRecipeId = null;
                    if (ingItem.getMetaforgeId() != null) {
                        ingRecipeId = recipeRepository.findByMetaforgeItemId(ingItem.getMetaforgeId())
                                .map(Recipe::getId)
                                .orElse(null);
                    }

                    // Prerequisite logic: Same ItemType AND has a recipe
                    // E.g. Anvil IV (Upgrade) uses Anvil III (Upgrade) + Materials
                    boolean isPrerequisite = ingItem.getItemType() != null
                            && ingItem.getItemType().equals(item.getItemType())
                            && ingRecipeId != null;

                    return new com.pauloneill.arcraidersplanner.dto.RecipeChainDto.RecipeIngredientChainDto(
                            ingItem.getId(),
                            ingItem.getName(),
                            ing.getQuantity(),
                            isPrerequisite,
                            ingRecipeId
                    );
                })
                .collect(java.util.stream.Collectors.toList());

        return new com.pauloneill.arcraidersplanner.dto.RecipeChainDto(
                item.getId(),
                item.getName(),
                recipe.getId(),
                ingredientDtos
        );
    }
}
