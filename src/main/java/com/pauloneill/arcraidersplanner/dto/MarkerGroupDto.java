package com.pauloneill.arcraidersplanner.dto;

import java.util.List;

/**
 * DTO for MarkerGroup zone information.
 */
public record MarkerGroupDto(
    Long id,
    String name,
    Long mapId,
    String mapName,
    ContainerTypeDto containerType,
    Double centerLat,
    Double centerLng,
    Integer markerCount,
    Double radius,
    List<String> markerIds // Individual marker IDs in this group
) {}
