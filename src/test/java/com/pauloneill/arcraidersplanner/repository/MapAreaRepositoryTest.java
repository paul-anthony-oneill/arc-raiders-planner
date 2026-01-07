package com.pauloneill.arcraidersplanner.repository;

import com.pauloneill.arcraidersplanner.model.Area;
import com.pauloneill.arcraidersplanner.model.GameMap;
import com.pauloneill.arcraidersplanner.model.LootType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MapAreaRepository
 * WHY: Validates custom JPQL query for zone highlighting works correctly with real database
 */
@DataJpaTest
class MapAreaRepositoryTest {

    @Autowired
    private MapAreaRepository mapAreaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldFindAreasByMapAndLootType() {
        // Given
        GameMap testMap = new GameMap();
        testMap.setName("Test Map");
        entityManager.persist(testMap);

        LootType mechanical = new LootType();
        mechanical.setName("Mechanical");
        mechanical.setDescription("Mechanical parts");
        entityManager.persist(mechanical);

        LootType industrial = new LootType();
        industrial.setName("Industrial");
        industrial.setDescription("Industrial materials");
        entityManager.persist(industrial);

        // Area 1: Has Mechanical
        Area area1 = createArea("Factory Floor", testMap, Set.of(mechanical));
        entityManager.persist(area1);

        // Area 2: Has both Mechanical and Industrial
        Area area2 = createArea("Assembly Line", testMap, Set.of(mechanical, industrial));
        entityManager.persist(area2);

        // Area 3: Only Industrial (should not be in results)
        Area area3 = createArea("Warehouse", testMap, Set.of(industrial));
        entityManager.persist(area3);

        entityManager.flush();

        // When
        List<Area> results = mapAreaRepository.findByMapAndLootType(testMap.getId(), "Mechanical");

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Area::getName)
                .containsExactlyInAnyOrder("Factory Floor", "Assembly Line");
    }

    @Test
    void shouldReturnEmptyListWhenNoAreasMatchLootType() {
        // Given
        GameMap testMap = new GameMap();
        testMap.setName("Empty Map");
        entityManager.persist(testMap);

        LootType mechanical = new LootType();
        mechanical.setName("Mechanical");
        mechanical.setDescription("Mechanical parts");
        entityManager.persist(mechanical);

        LootType industrial = new LootType();
        industrial.setName("Industrial");
        industrial.setDescription("Industrial materials");
        entityManager.persist(industrial);

        // Create area with only Industrial
        Area area = createArea("Industrial Zone", testMap, Set.of(industrial));
        entityManager.persist(area);

        entityManager.flush();

        // When - search for Mechanical
        List<Area> results = mapAreaRepository.findByMapAndLootType(testMap.getId(), "Mechanical");

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForNonexistentMap() {
        // When - search with invalid map ID
        List<Area> results = mapAreaRepository.findByMapAndLootType(999L, "Mechanical");

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void shouldHandleMultipleMapsWithSameLootType() {
        // Given
        LootType mechanical = new LootType();
        mechanical.setName("Mechanical");
        mechanical.setDescription("Mechanical parts");
        entityManager.persist(mechanical);

        // Map 1
        GameMap map1 = new GameMap();
        map1.setName("Map 1");
        entityManager.persist(map1);

        Area map1Area = createArea("Map 1 Area", map1, Set.of(mechanical));
        entityManager.persist(map1Area);

        // Map 2
        GameMap map2 = new GameMap();
        map2.setName("Map 2");
        entityManager.persist(map2);

        Area map2Area = createArea("Map 2 Area", map2, Set.of(mechanical));
        entityManager.persist(map2Area);

        entityManager.flush();

        // When - search only Map 1
        List<Area> results = mapAreaRepository.findByMapAndLootType(map1.getId(), "Mechanical");

        // Then - should only return Map 1 areas
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Map 1 Area");
    }

    @Test
    void shouldUseDistinctToAvoidDuplicates() {
        // Given
        GameMap testMap = new GameMap();
        testMap.setName("Test Map");
        entityManager.persist(testMap);

        LootType lootType1 = new LootType();
        lootType1.setName("TestType1");
        lootType1.setDescription("Type 1");
        entityManager.persist(lootType1);

        LootType lootType2 = new LootType();
        lootType2.setName("TestType2");
        lootType2.setDescription("Type 2");
        entityManager.persist(lootType2);

        // Area with multiple loot types including the search type
        Area area = createArea("Multi-Type Area", testMap, Set.of(lootType1, lootType2));
        entityManager.persist(area);

        entityManager.flush();

        // When
        List<Area> results = mapAreaRepository.findByMapAndLootType(testMap.getId(), "TestType1");

        // Then - should only return the area once (DISTINCT working)
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Multi-Type Area");
    }

    @Test
    void shouldFindByNameReturnsCorrectArea() {
        // Given
        GameMap testMap = new GameMap();
        testMap.setName("Test Map");
        entityManager.persist(testMap);

        Area area = createArea("Unique Area Name", testMap, new HashSet<>());
        entityManager.persist(area);

        entityManager.flush();

        // When
        var result = mapAreaRepository.findByName("Unique Area Name");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Unique Area Name");
    }

    private Area createArea(String name, GameMap map, Set<LootType> lootTypes) {
        Area area = new Area();
        area.setName(name);
        area.setGameMap(map);
        area.setMapX(100);
        area.setMapY(200);
        area.setLootAbundance(2);
        area.setLootTypes(lootTypes);
        return area;
    }
}
