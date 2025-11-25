package com.pauloneill.arcraidersplanner.dto;

import com.pauloneill.arcraidersplanner.model.MapMarker;

import java.util.List;

public record PlannerResponseDto(
        Long mapId,
        String mapName,
        double score,
        List<AreaDto> routePath,
        String extractionPoint,
        Double extractionLat,  // Calibrated Y coordinate of extraction point
        Double extractionLng,  // Calibrated X coordinate of extraction point
        List<EnemySpawnDto> nearbyEnemySpawns,  // All spawns of selected enemy types on this map, with onRoute status
        List<MapMarker> questMarkers
) {
}