package com.pauloneill.arcraidersplanner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pauloneill.arcraidersplanner.dto.HideoutUpgradeDto;
import com.pauloneill.arcraidersplanner.dto.MetaforgeItemDataResponse;
import com.pauloneill.arcraidersplanner.dto.MetaforgeItemDto;
import com.pauloneill.arcraidersplanner.dto.MetaforgeMapDataResponse;
import com.pauloneill.arcraidersplanner.dto.MetaforgeMarkerDto;
import com.pauloneill.arcraidersplanner.model.*;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import com.pauloneill.arcraidersplanner.repository.LootAreaRepository;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import com.pauloneill.arcraidersplanner.repository.RecipeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetaforgeSyncService {

    private final RestClient restClient;
    private final ItemRepository itemRepository;
    private final LootAreaRepository lootAreaRepository;
    private final MapMarkerRepository markerRepository;
    private final GameMapRepository gameMapRepository;
    private final RecipeRepository recipeRepository;
    private final CoordinateCalibrationService calibrationService;
    private final ObjectMapper objectMapper;
    private final MarkerGroupingService markerGroupingService; // NEW

    @Value("${metaforge.api.url}")
    private String metaforgeApiUrl;

    public MetaforgeSyncService(RestClient restClient, ItemRepository itemRepository,
            LootAreaRepository lootAreaRepository, MapMarkerRepository markerRepository,
            GameMapRepository gameMapRepository, RecipeRepository recipeRepository,
            CoordinateCalibrationService calibrationService, ObjectMapper objectMapper,
            MarkerGroupingService markerGroupingService) { // NEW
        this.restClient = restClient;
        this.itemRepository = itemRepository;
        this.lootAreaRepository = lootAreaRepository;
        this.markerRepository = markerRepository;
        this.gameMapRepository = gameMapRepository;
        this.recipeRepository = recipeRepository;
        this.calibrationService = calibrationService;
        this.objectMapper = objectMapper;
        this.markerGroupingService = markerGroupingService; // NEW
    }

    /**
     * Syncs items AND recipes from Metaforge API in a single pass.
     * WHY: Efficient - hits API once with includeComponents=true to get both item and recipe data
     */
    @Transactional
    public void syncItems() {
        int currentPage = 1;
        int totalPages = 1;
        int totalItemsSynced = 0;
        int recipesCreated = 0;
        int recipesUpdated = 0;
        int recipesSkipped = 0;

        // Cache existing LootTypes to avoid repeated DB lookups in loop
        Map<String, LootType> lootTypeCache = new HashMap<>();
        lootAreaRepository.findAll().forEach(lt -> lootTypeCache.put(lt.getName(), lt));

        do {
            // Add includeComponents=true to get recipe data in same response
            String uri = "/arc-raiders/items?includeComponents=true&page=" + currentPage;
            log.info("Fetching items and recipes from URI: {}{}", metaforgeApiUrl, uri);

            var response = restClient.get()
                    .uri(metaforgeApiUrl + uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<MetaforgeItemDataResponse<MetaforgeItemDto>>() {
                    });

            if (response == null || response.data() == null) {
                log.error("API returned null response for page {}", currentPage);
                break;
            }

            totalPages = response.pagination().totalPages();
            List<MetaforgeItemDto> externalItems = response.data();

            // PROCESS
            for (MetaforgeItemDto dto : externalItems) {
                String areaName = dto.lootAreaName();
                LootType lootType = null;

                if (areaName != null && !areaName.isBlank()) {
                    lootType = lootTypeCache.computeIfAbsent(areaName, name -> {
                        LootType newArea = new LootType();
                        newArea.setName(name);
                        return lootAreaRepository.save(newArea);
                    });
                }

                // Create/Update the Item Entity
                Optional<Item> existingItem = itemRepository.findByName(dto.name());
                Item itemToSave = getItemToSave(dto, existingItem, lootType);

                itemRepository.save(itemToSave);
                totalItemsSynced++;

                // Also process recipe data if components exist (same API call)
                if (dto.components() != null && !dto.components().isEmpty()) {
                    SyncResult result = syncCraftingRecipe(dto);
                    switch (result) {
                        case CREATED -> recipesCreated++;
                        case UPDATED -> recipesUpdated++;
                        case SKIPPED -> recipesSkipped++;
                    }
                }
            }

            currentPage++;

        } while (currentPage <= totalPages);

        log.info("Successfully synced {} items across {} pages.", totalItemsSynced, totalPages);
        log.info("Recipe sync complete: {} created, {} updated, {} skipped",
                recipesCreated, recipesUpdated, recipesSkipped);
                
        // Sync Workbench Upgrades from local JSONs
        syncWorkbenchUpgrades();
    }

    private Item getItemToSave(MetaforgeItemDto dto, Optional<Item> existingItem, LootType lootType) {
        Item itemToSave = existingItem.orElse(new Item());

        itemToSave.setName(dto.name());
        itemToSave.setDescription(dto.description());
        itemToSave.setRarity(dto.rarity());
        itemToSave.setItemType(dto.itemType());
        itemToSave.setIconUrl(dto.icon());
        itemToSave.setValue(dto.value());
        itemToSave.setLootType(lootType);
        // Link to Metaforge ID for ingredients lookup
        itemToSave.setMetaforgeId(dto.id());

        if (dto.stats() != null) {
            itemToSave.setWeight(dto.stats().weight());
            itemToSave.setStackSize(dto.stats().stackSize());
        }

        // Set workbench field from API (for Phase 3 workbench upgrade targeting)


        // Extract droppedBy information
        if (dto.droppedBy() != null && !dto.droppedBy().isEmpty()) {
            itemToSave.setDroppedBy(dto.droppedBy().stream()
                    .map(d -> d.arc().id())
                    .collect(Collectors.toSet()));
        } else {
            itemToSave.setDroppedBy(Collections.emptySet()); // Clear existing if none from Metaforge
        }

        return itemToSave;
    }

    @Transactional
    public void syncWorkbenchUpgrades() {
        log.info("--- STARTING WORKBENCH UPGRADE SYNC ---");
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath:data/hideout/*.json");
            log.info("Found {} hideout definition files", resources.length);

            for (Resource resource : resources) {
                try {
                    HideoutUpgradeDto dto = objectMapper.readValue(resource.getInputStream(), HideoutUpgradeDto.class);

                    // FILTER: Skip non-targetable workbenches
                    if (dto.maxLevel() == 0 || dto.levels().isEmpty()) {
                        log.info("Skipping {} - no upgrade levels", dto.id());
                        continue;
                    }

                    // FILTER: Skip if all levels use only coins (no item requirements)
                    if (hasOnlyCoinRequirements(dto)) {
                        log.info("Skipping {} - coin-based only", dto.id());
                        continue;
                    }

                    processHideoutUpgrade(dto);
                } catch (Exception e) {
                    log.error("Failed to parse hideout file: {}", resource.getFilename(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load hideout resources", e);
        }
        log.info("--- WORKBENCH UPGRADE SYNC COMPLETE ---");
    }

    private boolean hasOnlyCoinRequirements(HideoutUpgradeDto dto) {
        return dto.levels().stream()
                .allMatch(level -> level.requirementItemIds() == null || level.requirementItemIds().isEmpty());
    }

    private void processHideoutUpgrade(HideoutUpgradeDto dto) {
        String benchName = dto.name().getOrDefault("en", "Unknown Bench");
        
        for (HideoutUpgradeDto.UpgradeLevel level : dto.levels()) {
            String metaforgeId = "hideout_" + dto.id() + "_lvl" + level.level();
            String recipeName = benchName + " Level " + level.level();
            
            Optional<Recipe> existingRecipe = recipeRepository.findByMetaforgeItemId(metaforgeId);
            
            Recipe recipe;
            if (existingRecipe.isPresent()) {
                recipe = existingRecipe.get();
                recipe.getIngredients().clear();
            } else {
                recipe = new Recipe();
                recipe.setMetaforgeItemId(metaforgeId);
            }
            
            recipe.setName(recipeName);
            recipe.setDescription("Upgrade " + benchName + " to Level " + level.level());
            recipe.setType(RecipeType.WORKBENCH_UPGRADE);
            recipe.setIsRecyclable(false);
            
            int ingredientsAdded = 0;
            for (HideoutUpgradeDto.Requirement req : level.requirementItemIds()) {
                Optional<Item> itemOpt = itemRepository.findByMetaforgeId(req.itemId());
                
                if (itemOpt.isPresent()) {
                    RecipeIngredient ingredient = new RecipeIngredient();
                    ingredient.setItem(itemOpt.get());
                    ingredient.setQuantity(req.quantity());
                    recipe.addIngredient(ingredient);
                    ingredientsAdded++;
                } else {
                    log.warn("Missing ingredient for upgrade '{}': {} (Metaforge ID)", recipeName, req.itemId());
                }
            }
            
            if (ingredientsAdded > 0) {
                recipeRepository.save(recipe);
            }
        }
    }

    @Transactional
    public void syncMarkers() {
        log.info("--- STARTING MARKER SYNC ---");

        List<GameMap> maps = gameMapRepository.findAll();

        for (GameMap map : maps) {
            String mapApiCode = map.getDescription(); // e.g. "dam"
            log.info("Fetching markers for map: {}...", map.getName());

            try {
                var response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/game-map-data")
                                .queryParam("tableID", "arc_map_data")
                                .queryParam("mapID", mapApiCode)
                                .build())
                        .retrieve()
                        .body(MetaforgeMapDataResponse.class);

                // CHANGE 2: Extract from .allData()
                if (response == null || response.allData() == null || response.allData().isEmpty()) {
                    System.out.println("No markers found for " + map.getName());
                    continue;
                }

                List<MetaforgeMarkerDto> dtos = response.allData();

                System.out.println("Found " + dtos.size() + " markers for " + map.getName());

                for (MetaforgeMarkerDto dto : dtos) {
                    if (markerRepository.existsById(dto.id())) {
                        continue;
                    }

                    // Calibrate coordinates before storing
                    double[] calibrated = calibrationService.calibrateCoordinates(
                        dto.lat(),
                        dto.lng(),
                        map
                    );

                    MapMarker marker = new MapMarker();
                    marker.setId(dto.id());
                    marker.setLat(calibrated[0]);  // Store calibrated Y
                    marker.setLng(calibrated[1]);  // Store calibrated X
                    marker.setCategory(dto.category());
                    marker.setSubcategory(dto.subcategory());
                    marker.setName(dto.name());
                    marker.setGameMap(map);

                    markerRepository.save(marker);
                }
            } catch (Exception e) {
                log.error("Failed to sync markers for {}: {}", map.getName(), e.getMessage());
            }
            // After all markers for a map are synced, group them
            markerGroupingService.groupMarkersByContainer(map.getId()); // NEW
        }
        log.info("--- MARKER SYNC COMPLETE ---");
    }

    /**
     * Syncs a single crafting recipe from Metaforge item data.
     * WHY: Separates recipe sync logic for clarity and testability
     *
     * @param dto The Metaforge item DTO containing recipe data
     * @return SyncResult indicating whether recipe was created, updated, or skipped
     */
    private SyncResult syncCraftingRecipe(MetaforgeItemDto dto) {
        // Check if recipe already exists (idempotent sync)
        Optional<Recipe> existingRecipe = recipeRepository.findByMetaforgeItemId(dto.id());

        Recipe recipe;
        SyncResult result;

        if (existingRecipe.isPresent()) {
            recipe = existingRecipe.get();
            result = SyncResult.UPDATED;
            // Clear old ingredients for fresh update
            recipe.getIngredients().clear();
        } else {
            recipe = new Recipe();
            result = SyncResult.CREATED;
        }

        // Set recipe properties from API
        recipe.setName(dto.name());
        recipe.setDescription(dto.description());
        recipe.setType(RecipeType.CRAFTING);
        recipe.setMetaforgeItemId(dto.id());
        recipe.setIsRecyclable(false); // Phase 1: Only crafting recipes

        // Add ingredients by resolving component names to Item entities
        int ingredientsAdded = 0;
        for (var component : dto.components()) {
            String ingredientName = component.component().name();
            Optional<Item> ingredientItem = itemRepository.findByName(ingredientName);

            if (ingredientItem.isEmpty()) {
                log.warn("Ingredient '{}' not found for recipe '{}' - skipping ingredient",
                        ingredientName, dto.name());
                continue;
            }

            RecipeIngredient ingredient = new RecipeIngredient();
            ingredient.setItem(ingredientItem.get());
            ingredient.setQuantity(component.quantity());
            recipe.addIngredient(ingredient);
            ingredientsAdded++;
        }

        // Only save if we have valid ingredients
        if (ingredientsAdded == 0) {
            log.warn("Recipe '{}' has no valid ingredients - skipping", dto.name());
            return SyncResult.SKIPPED;
        }

        recipeRepository.save(recipe);
        return result;
    }

    /**
     * Result of syncing a single recipe.
     * WHY: Provides clear sync operation outcome for logging and metrics
     */
    private enum SyncResult {
        CREATED,  // New recipe created
        UPDATED,  // Existing recipe updated
        SKIPPED   // Recipe had no valid ingredients
    }
}