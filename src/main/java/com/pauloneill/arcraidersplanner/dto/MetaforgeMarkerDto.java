package com.pauloneill.arcraidersplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MetaforgeMarkerDto(
        String id,
        Double lat,
        Double lng,
        @JsonProperty("mapID") String mapId, // "dam", "buried_city"
        String category,
        String subcategory,
        @JsonProperty("instanceName") String name
) {
}