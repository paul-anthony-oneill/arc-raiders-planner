package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.ItemDto;
import com.pauloneill.arcraidersplanner.model.Item;

import java.util.List;

public interface ItemService {
    List<Item> searchItems(String query);

    List<Item> getAllItems();

    /**
     * Get item with full crafting/usage context.
     * WHY: Provides all data needed for center panel detail view in tactical planner UI
     *
     * @param itemId The item ID
     * @return ItemDto with crafting recipe, usage recipes, and drop sources
     */
    ItemDto getItemWithContext(Long itemId);

    List<String> getCraftableMetaforgeIds();

    com.pauloneill.arcraidersplanner.dto.RecipeChainDto getRecipeChain(Long itemId);
}
