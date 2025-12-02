package com.pauloneill.arcraidersplanner.dto;

import java.util.List;
import java.util.Set;

/**
 * DTO representing a waypoint in an optimized route.
 * Can represent either an Area or a MapMarker.
 */
public record WaypointDto(
        String id,
        String name,
        double x,
        double y,
        String type, // "AREA" or "MARKER"
        Set<String> lootTypes, // Only relevant for AREA type
        Integer lootAbundance, // Only relevant for AREA type
        String containerType,  // Only relevant for MARKER_GROUP type
        Integer markerCount,   // Only relevant for MARKER_GROUP type
        List<String> ongoingMatchItems,
        List<String> targetMatchItems
) {
}