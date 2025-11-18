package com.pauloneill.arcraidersplanner.dto;

public record MapRecommendationDto(
        Long mapId,
        String mapName,
        Long matchingAreaCount // The count of areas matching the loot type
) {}