package com.pauloneill.arcraidersplanner.dto;

import java.util.List;

public record PlannerRequestDto(
        List<String> targetItemNames,
        List<String> questMarkerIds,
        boolean hasRaiderKey,
        RoutingProfile routingProfile
) {
    public enum RoutingProfile {
        PURE_SCAVENGER, // Rank by pure count of matching areas
        EASY_EXFIL,     // Prioritize proximity to Raider Hatches
        AVOID_PVP,      // Prioritize map edges, penalize High Tier zone intersections
        SAFE_EXFIL      // Combined: Edge priority + High Tier avoidance + Raider Hatch proximity
    }
}