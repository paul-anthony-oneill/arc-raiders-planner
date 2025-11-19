package com.pauloneill.arcraidersplanner.repository;

import com.pauloneill.arcraidersplanner.model.MapMarker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MapMarkerRepository extends JpaRepository<MapMarker, String> {
    // Helper to find markers for a specific map
    List<MapMarker> findByGameMapId(Long mapId);
}