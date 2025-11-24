package com.pauloneill.arcraidersplanner.dto;

/**
 * Represents an ARC enemy for frontend display and selection.
 * WHY: Players need to target specific enemy spawns for loot farming and quest completion
 */
public record EnemyDto(
        String id,           // UUID from MapMarker
        String name,         // "Sentinel", "Guardian", etc.
        String type,         // subcategory
        String mapName,      // Which map it spawns on
        Double lat,          // Latitude coordinates
        Double lng,          // Longitude coordinates
        Integer threatLevel  // FUTURE: high/medium/low classification
) {
}
