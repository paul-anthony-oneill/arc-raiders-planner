package com.pauloneill.arcraidersplanner.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pauloneill.arcraidersplanner.model.Area;
import com.pauloneill.arcraidersplanner.model.MapMarker;
import com.pauloneill.arcraidersplanner.model.RoutablePoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Service for spatial calculations and geometry operations.
 * WHY: Extracts mathematical logic (distance, clustering, polygon analysis) from PlannerService,
 * enabling cleaner routing logic and future reuse for clustering algorithms.
 */
@Service
public class GeometryService {

    private static final Logger log = LoggerFactory.getLogger(GeometryService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Calculates the Euclidean distance between two RoutablePoints.
     */
    public double distance(RoutablePoint p1, RoutablePoint p2) {
        return Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2) + Math.pow(p1.getY() - p2.getY(), 2));
    }

    /**
     * Calculates the minimum distance from a point to a line segment AB.
     *
     * @param px Point X
     * @param py Point Y
     * @param ax Segment Start X
     * @param ay Segment Start Y
     * @param bx Segment End X
     * @param by Segment End Y
     * @return Minimum distance
     */
    public double pointToSegmentDistance(double px, double py, double ax, double ay, double bx, double by) {
        double l2 = Math.pow(bx - ax, 2) + Math.pow(by - ay, 2);
        if (l2 == 0)
            return Math.sqrt(Math.pow(px - ax, 2) + Math.pow(py - ay, 2));

        double t = ((px - ax) * (bx - ax) + (py - ay) * (by - ay)) / l2;
        t = Math.max(0, Math.min(1, t));

        double projX = ax + t * (bx - ax);
        double projY = ay + t * (by - ay);

        return Math.sqrt(Math.pow(px - projX, 2) + Math.pow(py - projY, 2));
    }

    /**
     * Calculates the minimum distance from a marker to any segment of the route path.
     *
     * @param marker Target marker (e.g. enemy)
     * @param path   Route path
     * @return Minimum distance
     */
    public double distanceToRoutePath(MapMarker marker, List<? extends RoutablePoint> path) {
        if (path.isEmpty()) {
            return Double.MAX_VALUE;
        }

        // If only one point, use point-to-point distance
        if (path.size() == 1) {
            return distance(path.get(0), marker);
        }

        // Calculate minimum distance to all route segments
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < path.size() - 1; i++) {
            RoutablePoint start = path.get(i);
            RoutablePoint end = path.get(i + 1);

            double segmentDist = pointToSegmentDistance(
                    marker.getX(), marker.getY(), // Point (enemy position)
                    start.getX(), start.getY(), // Segment start
                    end.getX(), end.getY() // Segment end
            );

            minDistance = Math.min(minDistance, segmentDist);
        }

        return minDistance;
    }

    /**
     * Calculates a safe radius for an area based on its polygon geometry.
     * Used for danger zone avoidance.
     *
     * @param area The area with polygon coordinates
     * @return Radius in map units
     */
    public double calculateDynamicRadius(Area area) {
        try {
            // Parse JSON: [[x,y], [x,y], ...]
            if (area.getCoordinates() == null || area.getCoordinates().isBlank()) {
                return 50.0;
            }
            List<List<Double>> coords = objectMapper.readValue(area.getCoordinates(), new TypeReference<>() {});
            if (coords.isEmpty())
                return 50.0;

            // Find max distance from center to any vertex
            double maxDist = 0;
            for (List<Double> point : coords) {
                // Based on V4: '[[462,-438]...]' -> Looks like [x, y]
                if (point.size() >= 2) {
                    double px = point.get(0);
                    double py = point.get(1);

                    double d = Math.sqrt(Math.pow(area.getX() - px, 2) + Math.pow(area.getY() - py, 2));
                    if (d > maxDist)
                        maxDist = d;
                }
            }
            return maxDist; // Safe buffer radius (or maybe add a buffer?)
        } catch (IOException e) {
            log.warn("Failed to parse coordinates for area {}: {}", area.getName(), e.getMessage());
            return 100.0; // Fallback default
        }
    }

    /**
     * Checks if the line between two areas intersects any High Tier Zone (danger zone).
     *
     * @param start Start area
     * @param end   End area
     * @param dangerZones List of danger zones
     * @return true if route is dangerous
     */
    public boolean isRouteDangerous(Area start, Area end, List<Area> dangerZones) {
        for (Area danger : dangerZones) {
            // Ignore if start or end IS the danger zone (we assume we are visiting it)
            if (danger.getId().equals(start.getId()) || danger.getId().equals(end.getId()))
                continue;

            double dangerRadius = calculateDynamicRadius(danger);
            double distToHazard = pointToSegmentDistance(
                    danger.getX(), danger.getY(),
                    start.getX(), start.getY(),
                    end.getX(), end.getY());

            if (distToHazard < dangerRadius)
                return true;
        }
        return false;
    }
}
