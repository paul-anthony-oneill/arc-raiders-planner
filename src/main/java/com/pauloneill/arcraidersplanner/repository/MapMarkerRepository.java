package com.pauloneill.arcraidersplanner.repository;

import com.pauloneill.arcraidersplanner.model.MapMarker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MapMarkerRepository extends JpaRepository<MapMarker, String> {
    // Helper to find markers for a specific map
    List<MapMarker> findByGameMapId(Long mapId);

    // Filter by category (e.g., "arc" for enemies)
    List<MapMarker> findByCategoryIgnoreCase(String category);

    // Search ARC enemies by name
    @Query("SELECT m FROM MapMarker m WHERE LOWER(m.category) = 'arc' AND LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<MapMarker> findArcEnemiesByName(@Param("name") String name);

    // Get specific markers by IDs (for planning)
    List<MapMarker> findByIdIn(List<String> ids);
}