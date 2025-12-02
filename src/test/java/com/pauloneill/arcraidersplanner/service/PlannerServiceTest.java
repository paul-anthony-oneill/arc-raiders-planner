package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.PlannerRequestDto;
import com.pauloneill.arcraidersplanner.dto.PlannerResponseDto;
import com.pauloneill.arcraidersplanner.dto.WaypointDto;
import com.pauloneill.arcraidersplanner.model.*;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import com.pauloneill.arcraidersplanner.service.TargetResolutionService.RecipeTargetInfo;
import com.pauloneill.arcraidersplanner.service.TargetResolutionService.TargetItemInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlannerServiceTest {

    @Mock
    private GameMapRepository gameMapRepository;
    @Mock
    private MapMarkerRepository mapMarkerRepository;
    @Mock
    private EnemyService enemyService;
    @Mock
    private TargetResolutionService targetResolutionService;

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
        mockTargetResolution("Copper Wire", "Industrial");

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
                List.of("Copper Wire"), null, Collections.emptyList(), false, PlannerRequestDto.RoutingProfile.PURE_SCAVENGER,
                Collections.emptyList()
        );

        // Act
        List<PlannerResponseDto> response = plannerService.generateRoute(request);

        // Assert
        assertEquals("Map A (Abundant)", response.getFirst().mapName());
        assertEquals(200.0, response.getFirst().score(), 0.1, "Score should be count * 100");
        assertEquals(2, response.getFirst().path().size());
    }

    @Test
    @DisplayName("AVOID PVP: Should penalize routes that cross High Tier (Abundance=1) zones")
    void testAvoidPvP_PenalizesDanger() {
        // Arrange
        mockTargetResolution("Copper Wire", "Industrial");

        // Map A: The "Dangerous" Map
        GameMap mapA = new GameMap();
        mapA.setId(1L);
        mapA.setName("Dangerous Map");
        Area startA = createArea(1L, 0, 0, 2, Set.of(industrial));
        Area endA = createArea(2L, 200, 0, 2, Set.of(industrial));
        Area danger = createArea(99L, 100, 0, 1, Set.of()); // Abundance 1 = High Tier
        mapA.setAreas(new HashSet<>(Arrays.asList(startA, endA, danger)));

        // Map B: The "Safe" Map
        GameMap mapB = new GameMap();
        mapB.setId(2L);
        mapB.setName("Safe Map");
        Area startB = createArea(3L, 0, 0, 2, Set.of(industrial));
        Area endB = createArea(4L, 200, 0, 2, Set.of(industrial));
        Area safeMiddle = createArea(98L, 100, 0, 3, Set.of()); // Abundance 3 = Low Tier
        mapB.setAreas(new HashSet<>(Arrays.asList(startB, endB, safeMiddle)));

        when(gameMapRepository.findAllWithAreas()).thenReturn(List.of(mapA, mapB));

        PlannerRequestDto request = new PlannerRequestDto(
                List.of("Copper Wire"), null, Collections.emptyList(), false, PlannerRequestDto.RoutingProfile.AVOID_PVP,
                Collections.emptyList()
        );

        // Act
        List<PlannerResponseDto> response = plannerService.generateRoute(request);

        // Assert
        assertEquals("Safe Map", response.get(0).mapName());
        assertTrue(response.get(0).score() > response.get(1).score(), "Safe map should outrank dangerous map");
    }

    @Test
    @DisplayName("EASY EXFIL: Should prioritize maps with a Raider Hatch near the loot")
    void testEasyExfil_PrioritizesHatch() {
        // Arrange
        mockTargetResolution("Copper Wire", "Industrial");

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
        hatchA.setSubcategory("hatch");
        hatchA.setLat(0.0);
        hatchA.setLng(10.0);
        hatchA.setName("Easy Hatch");

        MapMarker hatchB = new MapMarker();
        hatchB.setSubcategory("hatch");
        hatchB.setLat(0.0);
        hatchB.setLng(1000.0);
        hatchB.setName("Hard Hatch");

        when(mapMarkerRepository.findByGameMapId(1L)).thenReturn(List.of(hatchA));
        when(mapMarkerRepository.findByGameMapId(2L)).thenReturn(List.of(hatchB));

        PlannerRequestDto request = new PlannerRequestDto(
                List.of("Copper Wire"), null, Collections.emptyList(), true, PlannerRequestDto.RoutingProfile.EASY_EXFIL,
                Collections.emptyList()
        );

        // Act
        List<PlannerResponseDto> response = plannerService.generateRoute(request);

        // Assert
        assertEquals("Easy Exit Map", response.get(0).mapName());
        assertEquals("Easy Hatch", response.get(0).extractionPoint());
    }

    @Test
    @DisplayName("SAFE EXFIL: Should combine PvP avoidance with close Raider Hatch")
    void testSafeExfil_CombinesSafetyAndProximity() {
        // Arrange
        mockTargetResolution("Copper Wire", "Industrial");

        // Map A: "The Trap Map"
        GameMap mapA = new GameMap();
        mapA.setId(1L);
        mapA.setName("The Trap Map");
        Area lootA1 = createArea(1L, 0, 0, 2, Set.of(industrial));
        Area lootA2 = createArea(2L, 200, 0, 2, Set.of(industrial));
        Area dangerA = createArea(99L, 100, 0, 1, Set.of());
        mapA.setAreas(new HashSet<>(Arrays.asList(lootA1, lootA2, dangerA)));

        // Map B: "The Safe Map"
        GameMap mapB = new GameMap();
        mapB.setId(2L);
        mapB.setName("The Safe Map");
        Area lootB1 = createArea(3L, 0, 0, 2, Set.of(industrial));
        Area lootB2 = createArea(4L, 200, 200, 2, Set.of(industrial));
        Area safeZone = createArea(98L, 100, 100, 3, Set.of());
        mapB.setAreas(new HashSet<>(Arrays.asList(lootB1, lootB2, safeZone)));

        when(gameMapRepository.findAllWithAreas()).thenReturn(List.of(mapA, mapB));

        // Mock Raider Hatches
        MapMarker hatchA = new MapMarker();
        hatchA.setSubcategory("hatch");
        hatchA.setLat(0.0);
        hatchA.setLng(220.0);
        hatchA.setName("Danger Hatch");

        MapMarker hatchB = new MapMarker();
        hatchB.setSubcategory("hatch");
        hatchB.setLat(250.0);
        hatchB.setLng(250.0);
        hatchB.setName("Safe Hatch");

        when(mapMarkerRepository.findByGameMapId(1L)).thenReturn(List.of(hatchA));
        when(mapMarkerRepository.findByGameMapId(2L)).thenReturn(List.of(hatchB));

        PlannerRequestDto request = new PlannerRequestDto(
                List.of("Copper Wire"), null, Collections.emptyList(), true, PlannerRequestDto.RoutingProfile.SAFE_EXFIL,
                Collections.emptyList()
        );

        // Act
        List<PlannerResponseDto> response = plannerService.generateRoute(request);

        // Assert
        assertEquals("The Safe Map", response.get(0).mapName());
        assertEquals("Safe Hatch", response.get(0).extractionPoint());
    }

    // --- Helpers ---
    private void mockTargetResolution(String itemName, String lootType) {
        TargetItemInfo info = new TargetItemInfo(
                Set.of(lootType),
                Collections.<String>emptySet(),
                Collections.<String>emptySet(),
                Map.of(lootType, List.of(itemName)),
                Collections.<String, List<String>>emptyMap()
        );
        when(targetResolutionService.resolveTargetItems(eq(List.of(itemName)))).thenReturn(info);
        
        // Also mock recipe calls to empty
        RecipeTargetInfo emptyRecipeInfo = new RecipeTargetInfo(
                Collections.<String>emptySet(),
                Collections.<String, Set<String>>emptyMap(),
                Collections.<String, String>emptyMap(),
                Collections.<String>emptySet()
        );
        when(targetResolutionService.resolveRecipes(anyList())).thenReturn(emptyRecipeInfo);
        // And ingredient resolution (empty)
        when(targetResolutionService.resolveTargetItems(eq(new ArrayList<>()))).thenReturn(new TargetItemInfo(Collections.<String>emptySet(), Collections.<String>emptySet(), Collections.<String>emptySet(), Collections.<String, List<String>>emptyMap(), Collections.<String, List<String>>emptyMap()));
    }

    private Area createArea(Long id, int x, int y, int abundance, Set<LootType> lootTypes) {
        Area area = new Area();
        area.setId(id);
        area.setName("Area " + id + " (" + lootTypes.stream().map(LootType::getName).collect(Collectors.joining(", ")) + ")");
        area.setMapX(x);
        area.setMapY(y);
        area.setLootAbundance(abundance);
        area.setLootTypes(lootTypes);
        area.setCoordinates(String.format("[[%d,%d],[%d,%d]]", y - 5, x - 5, y + 5, x + 5));
        return area;
    }
}
