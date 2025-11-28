package com.pauloneill.arcraidersplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MetaforgeItemDto(
        String id,
        String name,
        String description,
        String rarity,
        String icon,
        Integer value,

        @JsonProperty("loot_area")
        String lootAreaName,

        @JsonProperty("item_type")
        String itemType,

        @JsonProperty("stat_block")
        StatBlock stats,

        // Recipe-related fields for crafting sync
        String workbench,

        @JsonProperty("components")
        List<RecipeComponent> components,

        @JsonProperty("recycle_components")
        List<RecipeComponent> recycleComponents,

        @JsonProperty("dropped_by")
        List<DroppedBy> droppedBy
) {
    // Inner record to catch the nested stats
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatBlock(
            Double weight,
            Integer stackSize
    ) {}

    // Recipe component structure from Metaforge API
    // Represents an ingredient with quantity
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecipeComponent(
            Integer quantity,
            ComponentItem component
    ) {}

    // Component item details (ingredient metadata)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ComponentItem(
            String id,
            String name,
            String icon,
            String rarity,

            @JsonProperty("item_type")
            String itemType
    ) {}

    // DroppedBy structure from Metaforge API
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DroppedBy(
            String id,
            @JsonProperty("arc")
            DroppedByArc arc
    ) {}

    // DroppedByArc structure from Metaforge API
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DroppedByArc(
            String id,
            String name
    ) {}
}