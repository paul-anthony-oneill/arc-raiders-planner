package com.pauloneill.arcraidersplanner.dto;

import java.util.List;

/**
 * Represents a specific enemy spawn location for map visualization.
 * WHY: Frontend needs to display all spawns of selected enemy types with route proximity highlighting
 */
public record EnemySpawnDto(
        String id,          // UUID from MapMarker
        String type,        // Enemy type (e.g., "sentinel", "guardian")
        String mapName,     // Which map this spawn is on
        Double lat,         // Latitude coordinates
        Double lng,         // Longitude coordinates
        Boolean onRoute,    // True if spawn is within proximity threshold of the route
        Double distanceToRoute,  // Distance in units from the nearest route point (null if not calculated)
        List<String> droppedItems // List of item names dropped by this enemy type
) {
}
