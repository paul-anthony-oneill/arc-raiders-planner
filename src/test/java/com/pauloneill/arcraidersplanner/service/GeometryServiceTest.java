package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.model.Area;
import com.pauloneill.arcraidersplanner.model.MapMarker;
import com.pauloneill.arcraidersplanner.model.RoutablePoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeometryServiceTest {

    private GeometryService geometryService;

    @BeforeEach
    void setUp() {
        geometryService = new GeometryService();
    }

    @Test
    @DisplayName("distance: Should calculate Euclidean distance correctly")
    void testDistance() {
        RoutablePoint p1 = createPoint(0, 0);
        RoutablePoint p2 = createPoint(3, 4);

        assertEquals(5.0, geometryService.distance(p1, p2), 0.001);
    }

    @Test
    @DisplayName("pointToSegmentDistance: Should calculate min distance to line segment")
    void testPointToSegmentDistance() {
        // Point (0, 1), Segment (-1, 0) to (1, 0) -> Distance should be 1
        double dist = geometryService.pointToSegmentDistance(0, 1, -1, 0, 1, 0);
        assertEquals(1.0, dist, 0.001);

        // Point (2, 0), Segment (0, 0) to (1, 0) -> Nearest point is (1,0), distance 1
        double dist2 = geometryService.pointToSegmentDistance(2, 0, 0, 0, 1, 0);
        assertEquals(1.0, dist2, 0.001);
    }

    @Test
    @DisplayName("Clustering: Should group nearby markers")
    void testClusterMarkersByProximity() {
        // Create two groups of markers
        // Group A: (0,0), (1,1), (2,2) - all within ~1.41 distance of neighbors
        MapMarker m1 = createMarker("1", 0, 0);
        MapMarker m2 = createMarker("2", 1, 1);
        MapMarker m3 = createMarker("3", 2, 2);

        // Group B: (100,100), (101,101) - far from A
        MapMarker m4 = createMarker("4", 100, 100);
        MapMarker m5 = createMarker("5", 101, 101);

        List<MapMarker> markers = List.of(m1, m2, m3, m4, m5);

        // Epsilon = 5.0, MinPoints = 2
        Map<Integer, List<MapMarker>> clusters = geometryService.clusterMarkersByProximity(markers, 5.0, 2);

        assertEquals(2, clusters.size());
        
        // Find which cluster contains m1
        List<MapMarker> clusterA = clusters.values().stream()
                .filter(c -> c.contains(m1))
                .findFirst()
                .orElseThrow();
        
        assertEquals(3, clusterA.size());
        assertTrue(clusterA.contains(m2));
        assertTrue(clusterA.contains(m3));

        // Find which cluster contains m4
        List<MapMarker> clusterB = clusters.values().stream()
                .filter(c -> c.contains(m4))
                .findFirst()
                .orElseThrow();
        
        assertEquals(2, clusterB.size());
        assertTrue(clusterB.contains(m5));
    }

    @Test
    @DisplayName("Clustering: Should handle isolated markers (noise)")
    void testClusterMarkers_Noise() {
        // Group A: (0,0), (1,1)
        MapMarker m1 = createMarker("1", 0, 0);
        MapMarker m2 = createMarker("2", 1, 1);

        // Noise: (50,50) - isolated
        MapMarker noise = createMarker("3", 50, 50);

        List<MapMarker> markers = List.of(m1, m2, noise);

        // Epsilon = 5.0, MinPoints = 2
        Map<Integer, List<MapMarker>> clusters = geometryService.clusterMarkersByProximity(markers, 5.0, 2);

        // Should have 2 entries in map: one list of 2 (cluster), one list of 1 (noise)
        assertEquals(2, clusters.size());

        boolean hasCluster = clusters.values().stream().anyMatch(c -> c.size() == 2);
        boolean hasNoise = clusters.values().stream().anyMatch(c -> c.size() == 1);

        assertTrue(hasCluster);
        assertTrue(hasNoise);
    }

    @Test
    @DisplayName("ClusterMetrics: Should calculate centroid and radius")
    void testCalculateClusterMetrics() {
        MapMarker m1 = createMarker("1", 0, 0);
        MapMarker m2 = createMarker("2", 10, 0);
        MapMarker m3 = createMarker("3", 0, 10);
        MapMarker m4 = createMarker("4", 10, 10);

        List<MapMarker> cluster = List.of(m1, m2, m3, m4);

        GeometryService.ClusterMetrics metrics = geometryService.calculateClusterMetrics(cluster);

        assertEquals(5.0, metrics.centerLng(), 0.001); // X
        assertEquals(5.0, metrics.centerLat(), 0.001); // Y
        
        // Max radius is distance from (5,5) to (0,0) -> sqrt(25+25) = sqrt(50) ~= 7.07
        // However, the service clamps minimum radius to 10.0
        assertEquals(10.0, metrics.radius(), 0.001);
        assertEquals(4, metrics.markerCount());
    }

    // --- Helpers ---

    private RoutablePoint createPoint(double x, double y) {
        return new RoutablePoint() {
            @Override public String getId() { return "p"; }
            @Override public double getX() { return x; }
            @Override public double getY() { return y; }
            @Override public String getName() { return "Point"; }
        };
    }

    private MapMarker createMarker(String id, double x, double y) {
        MapMarker m = new MapMarker();
        m.setId(id);
        m.setLng(x); // X
        m.setLat(y); // Y
        return m;
    }
}
