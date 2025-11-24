package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.EnemyDto;
import com.pauloneill.arcraidersplanner.model.MapMarker;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Service for managing ARC enemy data.
 * WHY: Players need to search and target specific enemy types for farming and
 * quest completion
 */
@Service
@Transactional(readOnly = true)
public class EnemyService {

    private static final String ARC_CATEGORY = "arc";

    /**
     * Larger enemies that should be included in targeting.
     * WHY: These enemies are higher value with specific spawns and specifically
     * required loot drops
     */
    private static final Set<String> INCLUDED_ENEMIES = Set.of(
            "queen",
            "leaper",
            "rocketeer",
            "sentinel",
            "bastion",
            "bombardier",
            "matriarch");

    private final MapMarkerRepository mapMarkerRepository;

    public EnemyService(MapMarkerRepository mapMarkerRepository) {
        this.mapMarkerRepository = mapMarkerRepository;
    }

    /**
     * Gets all ARC enemies across all maps.
     * WHY: Provides complete enemy catalog for frontend display
     *
     * @return List of all ARC enemies as DTOs
     */
    public List<EnemyDto> getAllArcEnemies() {
        List<MapMarker> markers = mapMarkerRepository.findByCategoryIgnoreCase(ARC_CATEGORY);
        return markers.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Searches ARC enemies by name.
     * WHY: Players need to quickly find specific enemy types (e.g., "Sentinel")
     *
     * @param query Search term (case-insensitive)
     * @return List of matching enemies
     */
    public List<EnemyDto> searchByName(String query) {
        if (query == null || query.isBlank()) {
            return getAllArcEnemies();
        }

        List<MapMarker> markers = mapMarkerRepository.findArcEnemiesByName(query);
        return markers.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Gets all ARC enemies for a specific map.
     * WHY: Map-specific enemy filtering for focused planning
     *
     * @param mapId The game map ID
     * @return List of enemies on the specified map
     */
    public List<EnemyDto> getEnemiesByMapId(Long mapId) {
        if (mapId == null) {
            return Collections.emptyList();
        }

        List<MapMarker> markers = mapMarkerRepository.findByGameMapId(mapId);
        return markers.stream()
                .filter(m -> ARC_CATEGORY.equalsIgnoreCase(m.getCategory()))
                .map(this::toDto)
                .toList();
    }

    /**
     * Gets specific enemies by their IDs.
     * WHY: Route planning needs actual MapMarker entities for waypoint calculation
     *
     * @param ids List of enemy UUIDs
     * @return List of MapMarker entities
     */
    public List<MapMarker> getEnemiesByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        return mapMarkerRepository.findByIdIn(ids);
    }

    /**
     * Gets distinct ARC enemy types available for targeting (excludes small flying
     * enemies).
     * WHY: Players select enemy types (e.g., "Sentinel"), not specific spawns
     *
     * @return List of selectable enemy type names
     */
    public List<String> getSelectableEnemyTypes() {
        List<MapMarker> allEnemies = mapMarkerRepository.findByCategoryIgnoreCase(ARC_CATEGORY);

        return allEnemies.stream()
                .map(MapMarker::getSubcategory)
                .filter(type -> type != null && INCLUDED_ENEMIES.contains(type.toLowerCase()))
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Gets all spawns of specified enemy types.
     * WHY: Route planning needs to find spawns of selected enemy types near loot
     * areas
     *
     * @param types List of enemy type names (subcategories)
     * @return List of MapMarker entities matching any of the specified types
     */
    public List<MapMarker> getSpawnsByTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return Collections.emptyList();
        }

        List<MapMarker> allEnemies = mapMarkerRepository.findByCategoryIgnoreCase(ARC_CATEGORY);

        // Convert types to lowercase for case-insensitive comparison
        Set<String> typesLower = types.stream()
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toSet());

        return allEnemies.stream()
                .filter(marker -> marker.getSubcategory() != null &&
                        typesLower.contains(marker.getSubcategory().toLowerCase()))
                .toList();
    }

    /**
     * Converts MapMarker entity to EnemyDto for frontend.
     * WHY: Decouples internal model from API contract
     *
     * @param marker The MapMarker entity
     * @return EnemyDto representation
     */
    private EnemyDto toDto(MapMarker marker) {
        return new EnemyDto(
                marker.getId(),
                marker.getName() != null ? marker.getName() : marker.getSubcategory(),
                marker.getSubcategory(),
                marker.getGameMap().getName(),
                marker.getLat(),
                marker.getLng(),
                null // FUTURE: Calculate threat level based on enemy type
        );
    }
}
