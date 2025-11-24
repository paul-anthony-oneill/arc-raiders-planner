package com.pauloneill.arcraidersplanner.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pauloneill.arcraidersplanner.model.Area;
import com.pauloneill.arcraidersplanner.repository.MapAreaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One-time service to fix Area mapX/mapY coordinates by calculating polygon centroids.
 * WHY: Initial data had incorrect marker positions that didn't match polygon boundaries.
 *
 * This service recalculates the center point (centroid) of each area's polygon
 * and updates the mapX/mapY values to ensure markers appear in the correct location.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.fix-area-coordinates", havingValue = "true", matchIfMissing = false)
public class AreaCoordinateFixService implements CommandLineRunner {

    private final MapAreaRepository mapAreaRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting area coordinate fix...");

        List<Area> allAreas = mapAreaRepository.findAll();
        int fixedCount = 0;
        int skippedCount = 0;

        for (Area area : allAreas) {
            if (area.getCoordinates() != null && !area.getCoordinates().isBlank()) {
                try {
                    // Parse coordinates JSON: [[lat1, lng1], [lat2, lng2], ...]
                    List<List<Double>> polygon = objectMapper.readValue(
                        area.getCoordinates(),
                        new TypeReference<List<List<Double>>>() {}
                    );

                    if (polygon.isEmpty()) {
                        log.warn("Area {} has empty coordinates array", area.getName());
                        skippedCount++;
                        continue;
                    }

                    // Calculate centroid (average of all vertices)
                    double sumLat = 0.0;
                    double sumLng = 0.0;

                    for (List<Double> vertex : polygon) {
                        if (vertex.size() >= 2) {
                            sumLat += vertex.get(0);
                            sumLng += vertex.get(1);
                        }
                    }

                    int centroidLat = (int) Math.round(sumLat / polygon.size());
                    int centroidLng = (int) Math.round(sumLng / polygon.size());

                    // Update only if values changed
                    Integer oldX = area.getMapX();
                    Integer oldY = area.getMapY();

                    area.setMapX(centroidLng);  // X = longitude
                    area.setMapY(centroidLat);  // Y = latitude

                    log.info("Fixed area '{}': ({}, {}) -> ({}, {})",
                        area.getName(), oldX, oldY, centroidLng, centroidLat);

                    fixedCount++;

                } catch (Exception e) {
                    log.error("Failed to parse coordinates for area {}: {}",
                        area.getName(), e.getMessage());
                    skippedCount++;
                }
            } else {
                log.debug("Area {} has no coordinates, skipping", area.getName());
                skippedCount++;
            }
        }

        mapAreaRepository.saveAll(allAreas);
        log.info("Area coordinate fix completed. Fixed: {}, Skipped: {}", fixedCount, skippedCount);
    }
}
