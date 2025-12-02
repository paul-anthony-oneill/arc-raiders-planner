package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.*;
import com.pauloneill.arcraidersplanner.model.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralized DTO mapping service.
 * WHY: Eliminates 50+ lines of boilerplate by consolidating all entity-to-DTO transformations
 * in one place, making maintenance easier when entity or DTO structures change.
 */
@Service
public class DtoMapper {

    /**
     * Converts Item entity to ItemDto.
     * WHY: Decouples internal model from API contract
     */
    public ItemDto toDto(Item item) {
        if (item == null) return null;

        ItemDto dto = new ItemDto();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setRarity(item.getRarity());
        dto.setItemType(item.getItemType());
        dto.setIconUrl(item.getIconUrl());
        dto.setValue(item.getValue());
        dto.setWeight(item.getWeight());
        dto.setStackSize(item.getStackSize());

        if (item.getLootType() != null) {
            dto.setLootType(item.getLootType().getName());
        }

        return dto;
    }

    /**
     * Converts Recipe entity to RecipeDto.
     * WHY: Provides frontend with recipe details and ingredient requirements
     */
    public RecipeDto toDto(Recipe recipe) {
        if (recipe == null) return null;

        List<RecipeIngredientDto> ingredients = recipe.getIngredients().stream()
                .map(ing -> new RecipeIngredientDto(
                        ing.getItem().getId(),
                        ing.getItem().getName(),
                        ing.getQuantity()
                ))
                .collect(Collectors.toList());

        return new RecipeDto(
                recipe.getId(),
                recipe.getMetaforgeItemId(),
                recipe.getName(),
                recipe.getDescription(),
                recipe.getType(),
                ingredients
        );
    }

    /**
     * Converts Area entity to AreaDto.
     * WHY: Maps loot area data for route planning display
     */
    public AreaDto toDto(Area area) {
        if (area == null) return null;

        AreaDto dto = new AreaDto();
        dto.setId(Long.valueOf(area.getId()));
        dto.setName(area.getName());
        dto.setMapX(area.getMapX());
        dto.setMapY(area.getMapY());
        dto.setCoordinates(area.getCoordinates());
        dto.setLootTypes(area.getLootTypes().stream()
                .map(LootType::getName)
                .collect(Collectors.toSet()));
        dto.setLootAbundance(area.getLootAbundance());
        return dto;
    }

    /**
     * Converts GameMap entity to GameMapDto with calibration data.
     * WHY: Provides frontend with map metadata and coordinate transformation constants
     */
    public GameMapDto toDto(GameMap map) {
        if (map == null) return null;

        GameMapDto dto = new GameMapDto();
        dto.setId(map.getId());
        dto.setName(map.getName());
        dto.setDescription(map.getDescription());
        dto.setImageUrl(map.getImageUrl());

        if (map.getAreas() != null) {
            dto.setAreas(map.getAreas().stream()
                    .map(this::toDto)
                    .collect(Collectors.toSet()));
        }

        dto.setCalibrationScaleX(map.getCalibrationScaleX());
        dto.setCalibrationScaleY(map.getCalibrationScaleY());
        dto.setCalibrationOffsetX(map.getCalibrationOffsetX());
        dto.setCalibrationOffsetY(map.getCalibrationOffsetY());

        return dto;
    }

    /**
     * Converts MapMarker entity to EnemyDto.
     * WHY: Transforms ARC enemy markers into frontend-friendly format
     */
    public EnemyDto toEnemyDto(MapMarker marker) {
        if (marker == null) return null;

        return new EnemyDto(
                marker.getId(),
                marker.getName() != null ? marker.getName() : marker.getSubcategory(),
                marker.getSubcategory(),
                marker.getGameMap().getName(),
                marker.getLat(),
                marker.getLng(),
                null // FUTURE: Calculate threat level based on enemy type
        );
    }

    /**
     * Converts ContainerType entity to ContainerTypeDto.
     */
    public ContainerTypeDto toDto(ContainerType containerType) {
        if (containerType == null) return null;

        return new ContainerTypeDto(
                containerType.getId(),
                containerType.getName(),
                containerType.getSubcategory(),
                containerType.getDescription(),
                containerType.getIconUrl()
        );
    }

    /**
     * Converts MarkerGroup entity to MarkerGroupDto.
     */
    public MarkerGroupDto toDto(MarkerGroup group) {
        if (group == null) return null;

        return new MarkerGroupDto(
                group.getDatabaseId(),
                group.getName(),
                group.getGameMap().getId(),
                group.getGameMap().getName(),
                toDto(group.getContainerType()),
                group.getCenterLat(),
                group.getCenterLng(),
                group.getMarkerCount(),
                group.getRadius(),
                group.getMarkers().stream()
                        .map(MapMarker::getId)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Batch converts Items to ItemDtos.
     * WHY: Convenience method for list operations
     */
    public List<ItemDto> toItemDtos(List<Item> items) {
        if (items == null) return Collections.emptyList();

        return items.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Batch converts Recipes to RecipeDtos.
     * WHY: Convenience method for list operations
     */
    public List<RecipeDto> toRecipeDtos(List<Recipe> recipes) {
        if (recipes == null) return Collections.emptyList();

        return recipes.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Batch converts MapMarkers to EnemyDtos.
     * WHY: Convenience method for list operations
     */
    public List<EnemyDto> toEnemyDtos(List<MapMarker> markers) {
        if (markers == null) return Collections.emptyList();

        return markers.stream()
                .map(this::toEnemyDto)
                .collect(Collectors.toList());
    }
}
