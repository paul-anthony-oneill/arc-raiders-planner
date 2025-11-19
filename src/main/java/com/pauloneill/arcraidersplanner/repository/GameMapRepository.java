package com.pauloneill.arcraidersplanner.repository;

import com.pauloneill.arcraidersplanner.dto.MapRecommendationDto;
import com.pauloneill.arcraidersplanner.model.GameMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameMapRepository extends JpaRepository<GameMap, Long> {

    @Query(value = """
                SELECT new com.pauloneill.arcraidersplanner.dto.MapRecommendationDto(
                    m.id,
                    m.name,
                    COUNT(a.id)
                )
                FROM GameMap m
                JOIN Area a ON a.gameMap.id = m.id
                JOIN a.lootTypes lt
                WHERE lt.name = :lootTypeName
                GROUP BY m.id, m.name
                ORDER BY COUNT(a.id) DESC
            """, nativeQuery = false)
    List<MapRecommendationDto> findMapsByLootTypeCount(
            @Param("lootTypeName") String lootTypeName);

    Optional<GameMap> findByName(String name);

    @Query("SELECT DISTINCT m FROM GameMap m JOIN FETCH m.areas a LEFT JOIN FETCH a.lootTypes WHERE m.name = :mapName")
    Optional<GameMap> findByNameWithAreas(@Param("mapName") String mapName);
}