package com.pauloneill.arcraidersplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
        StatBlock stats
) {
    // Inner record to catch the nested stats
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatBlock(
            Double weight,
            Integer stackSize
    ) {}
}