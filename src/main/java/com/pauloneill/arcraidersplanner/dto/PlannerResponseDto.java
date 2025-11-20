package com.pauloneill.arcraidersplanner.dto;

import java.util.List;

public record PlannerResponseDto(
        Long mapId,
        String mapName,
        double score,
        List<AreaDto> routePath,
        String extractionPoint) {
}