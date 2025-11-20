package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.PlannerRequestDto;
import com.pauloneill.arcraidersplanner.dto.PlannerResponseDto;
import com.pauloneill.arcraidersplanner.model.*;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlannerServiceTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private GameMapRepository gameMapRepository;
    @Mock
    private MapMarkerRepository mapMarkerRepository;

    @InjectMocks
    private PlannerService plannerService;

    // Test Data
    private LootType industrial;
    private Item copperWire;

    @BeforeEach
    void setUp() {
        industrial = new LootType();
        industrial.setName("Industrial");

        copperWire = new Item();
        copperWire.setName("Copper Wire");
        copperWire.setLootType(industrial);
    }

    @Test
    @DisplayName("PURE SCAVENGER: Should rank solely by number of matching areas, ignoring distance/tier")
    void testPureScavenger_CountsOnly() {
        // Arrange
        mockItem("Copper Wire", copperWire);

        // Map A: 2 Matches (Far apart)
        GameMap mapA = new GameMap();
        mapA.setId(1L);
        mapA.setName("Map A (Abundant)");
        Area a1 = createArea(10L, 0, 0, 2, Set.of(industrial));
        Area a2 = createArea(11L, 1000, 1000, 2, Set.of(industrial)); // Far away
        mapA.setAreas(new HashSet<>(Arrays.asList(a1, a2)));

        // Map B: 1 Match (Close)
        GameMap mapB = new GameMap();
        mapB.setId(2L);
        mapB.setName("Map B (Sparse)");
        Area b1 = createArea(20L, 0, 0, 2, Set.of(industrial));
        mapB.setAreas(new HashSet<>(List.of(b1)));

        when(gameMapRepository.findAllWithAreas()).thenReturn(List.of(mapA, mapB));

        PlannerRequestDto request = new PlannerRequestDto(
                List.of("Copper Wire"), null, false, PlannerRequestDto.RoutingProfile.PURE_SCAVENGER
        );

        // Act
        List<PlannerResponseDto> response = plannerService.generateRoute(request);

        // Assert
        // Map A should win because 2 > 1. Distance penalty is ignored in PURE_SCAVENGER.
        assertEquals("Map A (Abundant)", response.getFirst().mapName());
        assertEquals(200.0, response.getFirst().score(), 0.1, "Score should be count * 100");
    }

    @Test
    @DisplayName("AVOID PVP: Should penalize routes that cross High Tier (Abundance=1) zones")
    void testAvoidPvP_PenalizesDanger() {
        // Arrange
        mockItem("Copper Wire", copperWire);

        // Map A: The "Dangerous" Map
        // Path goes from (0,0) to (200,0).
        // A High Tier Zone sits at (100,0) with a radius. The path intersects it.
        GameMap mapA = new GameMap();
        mapA.setId(1L);
        mapA.setName("Dangerous Map");
        Area startA = createArea(1L, 0, 0, 2, Set.of(industrial));
        Area endA = createArea(2L, 200, 0, 2, Set.of(industrial));
        // High Tier Zone in the middle
        Area danger = createArea(99L, 100, 0, 1, Set.of()); // Abundance 1 = High Tier
        mapA.setAreas(new HashSet<>(Arrays.asList(startA, endA, danger)));

        // Map B: The "Safe" Map
        // Identical layout, but the middle zone is Low Tier (Abundance=3)
        GameMap mapB = new GameMap();
        mapB.setId(2L);
        mapB.setName("Safe Map");
        Area startB = createArea(3L, 0, 0, 2, Set.of(industrial));
        Area endB = createArea(4L, 200, 0, 2, Set.of(industrial));
        Area safeMiddle = createArea(98L, 100, 0, 3, Set.of()); // Abundance 3 = Low Tier
        mapB.setAreas(new HashSet<>(Arrays.asList(startB, endB, safeMiddle)));

        when(gameMapRepository.findAllWithAreas()).thenReturn(List.of(mapA, mapB));

        PlannerRequestDto request = new PlannerRequestDto(
                List.of("Copper Wire"), null, false, PlannerRequestDto.RoutingProfile.AVOID_PVP
        );

        // Act
        List<PlannerResponseDto> response = plannerService.generateRoute(request);

        // Assert
        // Map B should win significantly because Map A gets a -200 (or -500) penalty for the danger zone.
        assertEquals("Safe Map", response.get(0).mapName());
        assertTrue(response.get(0).score() > response.get(1).score(), "Safe map should outrank dangerous map");
    }

    @Test
    @DisplayName("EASY EXFIL: Should prioritize maps with a Raider Hatch near the loot")
    void testEasyExfil_PrioritizesHatch() {
        // Arrange
        mockItem("Copper Wire", copperWire);

        // Map A: Loot is at (0,0). Hatch is at (10,0) -> Very Close.
        GameMap mapA = new GameMap();
        mapA.setId(1L);
        mapA.setName("Easy Exit Map");
        Area lootA = createArea(1L, 0, 0, 2, Set.of(industrial));
        mapA.setAreas(new HashSet<>(List.of(lootA)));

        // Map B: Loot is at (0,0). Hatch is at (1000,0) -> Far away.
        GameMap mapB = new GameMap();
        mapB.setId(2L);
        mapB.setName("Hard Exit Map");
        Area lootB = createArea(2L, 0, 0, 2, Set.of(industrial));
        mapB.setAreas(new HashSet<>(List.of(lootB)));

        when(gameMapRepository.findAllWithAreas()).thenReturn(List.of(mapA, mapB));

        // Mock Markers
        MapMarker hatchA = new MapMarker();
        hatchA.setCategory("Raider Hatch");
        hatchA.setLat(0.0); // Y
        hatchA.setLng(10.0); // X (Close)
        hatchA.setName("Easy Hatch");

        MapMarker hatchB = new MapMarker();
        hatchB.setCategory("Raider Hatch");
        hatchB.setLat(0.0);
        hatchB.setLng(1000.0); // X (Far)
        hatchB.setName("Hard Hatch");

        when(mapMarkerRepository.findByGameMapId(1L)).thenReturn(List.of(hatchA));
        when(mapMarkerRepository.findByGameMapId(2L)).thenReturn(List.of(hatchB));

        PlannerRequestDto request = new PlannerRequestDto(
                List.of("Copper Wire"), null, true, PlannerRequestDto.RoutingProfile.EASY_EXFIL
        );

        // Act
        List<PlannerResponseDto> response = plannerService.generateRoute(request);

        // Assert
        assertEquals("Easy Exit Map", response.get(0).mapName());
        assertEquals("Easy Hatch", response.get(0).extractionPoint());
        assertTrue(response.get(0).score() > response.get(1).score());
    }

    // --- Helpers ---
    private void mockItem(String name, Item item) {
        when(itemRepository.findByName(name)).thenReturn(Optional.of(item));
    }

    private Area createArea(Long id, int x, int y, int abundance, Set<LootType> lootTypes) {
        Area area = new Area();
        area.setId(id);
        area.setMapX(x);
        area.setMapY(y);
        area.setLootAbundance(abundance);
        area.setLootTypes(lootTypes);
        // Valid JSON for radius calculation
        area.setCoordinates(String.format("[[%d,%d],[%d,%d]]", y - 5, x - 5, y + 5, x + 5));
        return area;
    }
}