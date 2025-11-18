package com.pauloneill.arcraidersplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MetaforgeItemDto(
        String id,
        String name,
        String description,
        String rarity,

        @JsonProperty("loot_area")
        String lootAreaName,

        @JsonProperty("item_type")
        String itemType
) {
}