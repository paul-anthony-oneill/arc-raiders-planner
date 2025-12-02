package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.model.ContainerType;
import com.pauloneill.arcraidersplanner.model.GameMap;
import com.pauloneill.arcraidersplanner.model.MapMarker;
import com.pauloneill.arcraidersplanner.model.MarkerGroup;
import com.pauloneill.arcraidersplanner.repository.ContainerTypeRepository;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import com.pauloneill.arcraidersplanner.repository.MarkerGroupRepository;
import com.pauloneill.arcraidersplanner.service.GeometryService.ClusterMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service to group individual MapMarkers into MarkerGroups (clusters) based on container type and proximity.
 * WHY: Reduces routing noise by consolidating many individual container spawns into logical zones for the planner.
 */
@Service
@Slf4j
public class MarkerGroupingService {

    private final MapMarkerRepository mapMarkerRepository;
    private final MarkerGroupRepository markerGroupRepository;
    private final ContainerTypeRepository containerTypeRepository;
    private final GameMapRepository gameMapRepository;
    private final GeometryService geometryService;

    public MarkerGroupingService(MapMarkerRepository mapMarkerRepository,
                                 MarkerGroupRepository markerGroupRepository,
                                 ContainerTypeRepository containerTypeRepository,
                                 GameMapRepository gameMapRepository,
                                 GeometryService geometryService) {
        this.mapMarkerRepository = mapMarkerRepository;
        this.markerGroupRepository = markerGroupRepository;
        this.containerTypeRepository = containerTypeRepository;
        this.gameMapRepository = gameMapRepository;
        this.geometryService = geometryService;
    }

    /**
     * Groups markers by container type and proximity for a given map.
     * Creates new MarkerGroup entities for identified clusters and updates individual MapMarkers.
     * WHY: Centralized logic for maintaining logical container zones.
     *
     * @param mapId The ID of the GameMap to process.
     */
    @Transactional
    public void groupMarkersByContainer(Long mapId) {
        log.info("Starting marker grouping for mapId: {}", mapId);

        Optional<GameMap> gameMapOptional = gameMapRepository.findById(mapId);
        if (gameMapOptional.isEmpty()) {
            log.warn("GameMap with ID {} not found. Skipping grouping.", mapId);
            return;
        }
        GameMap gameMap = gameMapOptional.get();

        // Clear existing groups for this map to regenerate
        markerGroupRepository.findByGameMapId(mapId).forEach(group -> {
            mapMarkerRepository.findByMarkerGroup(group).forEach(marker -> {
                marker.setMarkerGroup(null);
                marker.setIsGrouped(false);
                marker.setStandaloneReason(null);
                mapMarkerRepository.save(marker);
            });
            markerGroupRepository.delete(group);
        });

        List<ContainerType> containerTypes = containerTypeRepository.findAll();
        int totalGroupsCreated = 0;
        int totalMarkersGrouped = 0;
        int totalStandaloneMarkers = 0;

        for (ContainerType containerType : containerTypes) {
            // Get all markers of this type on this map that are not already grouped (by other mechanisms)
            // and are not special POIs (extraction, raider hatch etc.)
            List<MapMarker> eligibleMarkers = mapMarkerRepository
                    .findByGameMapIdAndSubcategoryAndCategoryNot(mapId, containerType.getSubcategory(), "poi");

            if (eligibleMarkers.isEmpty()) {
                continue;
            }

            // Cluster by proximity using GeometryService
            double maxDistance = calculateMaxDistance(containerType);
            // minClusterSize = 2 as per design doc, otherwise they become standalone.
            Map<Integer, List<MapMarker>> clusters = geometryService
                    .clusterMarkersByProximity(eligibleMarkers, maxDistance, 2);

            for (List<MapMarker> clusterMarkers : clusters.values()) {
                if (clusterMarkers.size() == 1) {
                    // This is a standalone marker (noise in DBSCAN terms relative to minClusterSize)
                    MapMarker standalone = clusterMarkers.getFirst(); // Java 21+
                    standalone.setIsGrouped(false);
                    standalone.setStandaloneReason("isolated");
                    mapMarkerRepository.save(standalone);
                    totalStandaloneMarkers++;
                } else {
                    // This is a valid cluster, create a MarkerGroup
                    ClusterMetrics metrics = geometryService.calculateClusterMetrics(clusterMarkers);
                    MarkerGroup group = new MarkerGroup();
                    group.setName(generateGroupName(gameMap.getName(), containerType, totalGroupsCreated + 1));
                    group.setGameMap(gameMap);
                    group.setContainerType(containerType);
                    group.setCenterLat(metrics.centerLat());
                    group.setCenterLng(metrics.centerLng());
                    group.setRadius(metrics.radius());
                    group.setMarkerCount(metrics.markerCount());
                    MarkerGroup savedGroup = markerGroupRepository.save(group);
                    totalGroupsCreated++;

                    // Link markers to the new group
                    for (MapMarker marker : clusterMarkers) {
                        marker.setMarkerGroup(savedGroup);
                        marker.setIsGrouped(true);
                        marker.setStandaloneReason(null);
                        mapMarkerRepository.save(marker);
                        totalMarkersGrouped++;
                    }
                }
            }
        }
        log.info("Finished marker grouping for mapId {}. Created {} groups, grouped {} markers, {} standalone markers.",
                mapId, totalGroupsCreated, totalMarkersGrouped, totalStandaloneMarkers);
    }

    /**
     * Calculates the maximum distance (epsilon) for clustering based on the container type.
     * WHY: Different container types may have different natural densities.
     *
     * @param containerType The type of container.
     * @return The maximum distance for clustering.
     */
    public double calculateMaxDistance(ContainerType containerType) {
        // Different container types have different clustering radii as per design doc
        return switch (containerType.getSubcategory()) {
            case "red-locker", "locker" -> 50.0; // Tight clustering for lockers
            case "raider-cache" -> 100.0;        // Moderate for caches
            case "weapon-crate" -> 75.0;
            default -> 60.0;
        };
    }

    /**
     * Generates a descriptive name for a marker group.
     * WHY: Provides a user-friendly label for the clustered zone.
     *
     * @param mapName The name of the game map.
     * @param type The container type of the group.
     * @param clusterIndex A unique index for the cluster on the map.
     * @return A formatted group name.
     */
    public String generateGroupName(String mapName, ContainerType type, int clusterIndex) {
        return String.format("%s - %s Zone %d",
                mapName.replace("_", " "),
                type.getName(),
                clusterIndex
        );
    }
}
