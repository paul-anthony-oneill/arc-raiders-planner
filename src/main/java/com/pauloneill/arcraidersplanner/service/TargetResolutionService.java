package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.model.MarkerGroup;
import com.pauloneill.arcraidersplanner.model.Recipe;
import com.pauloneill.arcraidersplanner.repository.ContainerTypeRepository;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import com.pauloneill.arcraidersplanner.repository.MarkerGroupRepository;
import com.pauloneill.arcraidersplanner.repository.RecipeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for resolving target entities (items, recipes, and future containers) to their requirements.
 * WHY: Extracted from PlannerService to separate target resolution logic from routing algorithms.
 * Named "TargetResolutionService" (not "ItemResolutionService") because it will be extended
 * to support container targeting in future phases.
 */
@Service
@Transactional(readOnly = true)
public class TargetResolutionService {

    private static final Logger log = LoggerFactory.getLogger(TargetResolutionService.class);

    private final ItemRepository itemRepository;
    private final RecipeRepository recipeRepository;
    private final ContainerTypeRepository containerTypeRepository; // NEW
    private final MarkerGroupRepository markerGroupRepository;     // NEW

    public TargetResolutionService(ItemRepository itemRepository, RecipeRepository recipeRepository,
                                   ContainerTypeRepository containerTypeRepository,
                                   MarkerGroupRepository markerGroupRepository) { // NEW
        this.itemRepository = itemRepository;
        this.recipeRepository = recipeRepository;
        this.containerTypeRepository = containerTypeRepository;
        this.markerGroupRepository = markerGroupRepository;
    }

    /**
     * Resolves target items to their loot types and enemy drop sources.
     * WHY: Planner needs to know which loot areas and enemy spawns are relevant for the target items
     *
     * @param itemNames List of item names to resolve
     * @return TargetItemInfo with loot types, enemy types, and mappings
     */
    public TargetItemInfo resolveTargetItems(List<String> itemNames) {
        Set<String> targetLootTypes = new HashSet<>();
        Set<String> targetDroppedByEnemies = new HashSet<>();
        Set<String> exclusiveDroppedByEnemies = new HashSet<>();
        Map<String, List<String>> lootTypeToItemNames = new HashMap<>();
        Map<String, List<String>> enemyTypeToItemNames = new HashMap<>();

        if (itemNames != null && !itemNames.isEmpty()) {
            for (String name : itemNames) {
                itemRepository.findByName(name)
                        .ifPresent(item -> {
                            boolean hasLootType = item.getLootType() != null;
                            boolean hasDroppedBy = item.getDroppedBy() != null && !item.getDroppedBy().isEmpty();

                            if (hasLootType) {
                                targetLootTypes.add(item.getLootType().getName());
                                lootTypeToItemNames.computeIfAbsent(item.getLootType().getName(), k -> new ArrayList<>()).add(name);
                            }
                            if (hasDroppedBy) {
                                targetDroppedByEnemies.addAll(item.getDroppedBy());
                                item.getDroppedBy().forEach(enemyType ->
                                        enemyTypeToItemNames.computeIfAbsent(enemyType, k -> new ArrayList<>()).add(name));

                                if (!hasLootType) { // If item ONLY drops from enemy
                                    exclusiveDroppedByEnemies.addAll(item.getDroppedBy());
                                }
                            }
                        });
            }
        }
        return new TargetItemInfo(targetLootTypes, targetDroppedByEnemies, exclusiveDroppedByEnemies, lootTypeToItemNames, enemyTypeToItemNames);
    }

    /**
     * Resolves target recipes to their ingredient requirements.
     * WHY: Planner needs to know which items are required for crafting/upgrades
     *
     * @param recipeIds List of recipe Metaforge IDs to resolve
     * @return RecipeTargetInfo with ingredients and mappings
     */
    public RecipeTargetInfo resolveRecipes(List<String> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return new RecipeTargetInfo(
                    Collections.emptySet(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptySet()
            );
        }

        Map<String, Set<String>> recipeToIngredients = new HashMap<>();
        Map<String, String> recipeToDisplayName = new HashMap<>();
        Set<String> allIngredients = new HashSet<>();

        for (String recipeId : recipeIds) {
            Optional<Recipe> recipe = recipeRepository.findByMetaforgeItemId(recipeId);

            if (recipe.isEmpty()) {
                log.warn("Recipe not found: {}", recipeId);
                continue;
            }

            Recipe targetRecipe = recipe.get();
            Set<String> ingredients = targetRecipe.getIngredients().stream()
                    .map(ing -> ing.getItem().getName())
                    .collect(Collectors.toSet());

            recipeToIngredients.put(recipeId, ingredients);
            recipeToDisplayName.put(recipeId, targetRecipe.getName());
            allIngredients.addAll(ingredients);
        }

        return new RecipeTargetInfo(
                new HashSet<>(recipeIds),
                recipeToIngredients,
                recipeToDisplayName,
                allIngredients
        );
    }

    /**
     * Resolves ongoing items to their loot type mappings.
     * WHY: Used to display which ongoing items are available in each loot area
     *
     * @param itemNames List of ongoing item names
     * @return Map of loot type name to list of item names
     */
    public Map<String, List<String>> resolveOngoingItems(List<String> itemNames) {
        if (itemNames == null || itemNames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> map = new HashMap<>();
        for (String name : itemNames) {
            itemRepository.findByName(name)
                    .ifPresent(item -> {
                        if (item.getLootType() != null) {
                            map.computeIfAbsent(item.getLootType().getName(), k -> new ArrayList<>()).add(name);
                        }
                    });
        }
        return map;
    }

    /**
     * Resolves loot types for a set of item names.
     * WHY: Used to identify which loot types contain the required recipe ingredients.
     *
     * @param itemNames Set of item names (e.g. ingredients)
     * @return Set of LootType names
     */
    public Set<String> getLootTypesForItems(Set<String> itemNames) {
        Set<String> lootTypes = new HashSet<>();
        if (itemNames != null && !itemNames.isEmpty()) {
            for (String name : itemNames) {
                itemRepository.findByName(name).ifPresent(item -> {
                    if (item.getLootType() != null) {
                        lootTypes.add(item.getLootType().getName());
                    }
                });
            }
        }
        return lootTypes;
    }

    /**
     * Resolves target containers to their marker groups for a specific map.
     * WHY: Planner needs to route through container spawn zones.
     *
     * @param containerSubcategories List of container type subcategories (e.g., "red-locker", "raider-cache")
     * @param mapId The ID of the map to filter container groups by.
     * @return ContainerTargetInfo with relevant MarkerGroups.
     */
    public ContainerTargetInfo resolveTargetContainers(List<String> containerSubcategories, Long mapId) {
        if (containerSubcategories == null || containerSubcategories.isEmpty() || mapId == null) {
            return new ContainerTargetInfo(Collections.emptyList());
        }

        List<MarkerGroup> relevantGroups = new ArrayList<>();
        for (String subcategory : containerSubcategories) {
            containerTypeRepository.findBySubcategory(subcategory)
                    .ifPresent(containerType -> {
                        relevantGroups.addAll(markerGroupRepository.findByGameMapIdAndContainerType(mapId, containerType));
                    });
        }
        return new ContainerTargetInfo(relevantGroups);
    }

    /**
     * Information about target items and their sources.
     * WHY: Aggregates all item-related targeting data for the planner
     */
    public record TargetItemInfo(
            Set<String> targetLootTypes,
            Set<String> targetDroppedByEnemies,
            Set<String> exclusiveDroppedByEnemies, // Enemies that ONLY drop item, no loot area
            Map<String, List<String>> lootTypeToItemNames,
            Map<String, List<String>> enemyTypeToItemNames
    ) {}

    /**
     * Information about target recipes and their ingredients.
     * WHY: Aggregates all recipe-related targeting data for the planner
     */
    public record RecipeTargetInfo(
            Set<String> recipeIds,                           // Requested recipe IDs
            Map<String, Set<String>> recipeToIngredientNames, // ID → ingredient names
            Map<String, String> recipeToDisplayName,          // ID → "Gunsmith Level 2" or "Battery"
            Set<String> allIngredientNames                    // Flattened set for resolution
    ) {}

    /**
     * Information about target containers.
     * WHY: Aggregates all container-related targeting data for the planner.
     */
    public record ContainerTargetInfo(
            List<MarkerGroup> markerGroups // List of MarkerGroup entities that match the target container types
    ) {}
}
