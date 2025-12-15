package com.pauloneill.arcraidersplanner.repository;

import com.pauloneill.arcraidersplanner.model.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Area entity operations.
 * WHY: Provides data access for loot areas and zone highlighting
 */
public interface MapAreaRepository extends JpaRepository<Area, Long> {
    Optional<Area> findByName(String name);

    /**
     * Find all areas on a map that have a specific loot type.
     * WHY: Used for zone highlighting by item selection in tactical planner.
     *
     * @param mapId The map ID
     * @param lootTypeName The loot type name (e.g., "Mechanical", "Industrial")
     * @return List of areas that contain this loot type on the specified map
     */
    @Query("SELECT DISTINCT a FROM Area a " +
           "JOIN a.lootTypes lt " +
           "WHERE a.gameMap.id = :mapId AND lt.name = :lootTypeName")
    List<Area> findByMapAndLootType(
            @Param("mapId") Long mapId,
            @Param("lootTypeName") String lootTypeName
    );
}