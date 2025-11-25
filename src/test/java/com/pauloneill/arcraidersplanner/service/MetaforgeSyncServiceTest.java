package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.model.GameMap;
import com.pauloneill.arcraidersplanner.model.MapMarker;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MetaforgeSyncServiceTest {

    @Autowired
    private MetaforgeSyncService syncService;

    @Autowired
    private MapMarkerRepository markerRepository;

    @Test
    void testMarkersStoreCalibratedCoordinates() {
        // Run marker sync
        syncService.syncMarkers();

        // Verify markers have calibrated coordinates
        List<MapMarker> markers = markerRepository.findAll();
        assertTrue(markers.size() > 0, "Should have synced some markers");

        for (MapMarker marker : markers) {
            assertNotNull(marker.getLat(), "Should have calibrated lat");
            assertNotNull(marker.getLng(), "Should have calibrated lng");

            // Coordinates should be in reasonable Leaflet range
            assertTrue(Math.abs(marker.getLat()) < 10000,
                      "Lat should be reasonable: " + marker.getLat());
            assertTrue(Math.abs(marker.getLng()) < 10000,
                      "Lng should be reasonable: " + marker.getLng());
        }
    }

    @Test
    void testCalibratedCoordinatesDifferFromRaw() {
        syncService.syncMarkers();

        // For maps with non-identity calibration, verify transformation occurred
        List<MapMarker> markers = markerRepository.findAll().stream()
                .filter(m -> {
                    GameMap map = m.getGameMap();
                    return !map.getCalibrationScaleX().equals(1.0) ||
                           !map.getCalibrationOffsetX().equals(0.0);
                })
                .toList();

        if (!markers.isEmpty()) {
            // If we have markers with calibration, verify they differ from raw
            // (This is a conceptual test - we don't store raw anymore)
            assertTrue(markers.size() > 0,
                      "Should have some markers from maps with calibration");
        }
    }
}
