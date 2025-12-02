package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.PlannerRequestDto;
import com.pauloneill.arcraidersplanner.dto.PlannerResponseDto;
import com.pauloneill.arcraidersplanner.dto.WaypointDto;
import com.pauloneill.arcraidersplanner.model.Area;
import com.pauloneill.arcraidersplanner.model.GameMap;
import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.model.LootType;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import com.pauloneill.arcraidersplanner.service.TargetResolutionService.RecipeTargetInfo;
import com.pauloneill.arcraidersplanner.service.TargetResolutionService.TargetItemInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlannerServiceOngoingItemsTest {

    @Mock
    private GameMapRepository gameMapRepository;
    @Mock
    private MapMarkerRepository mapMarkerRepository;
    @Mock
    private EnemyService enemyService;
    @Mock
    private TargetResolutionService targetResolutionService;

    private GeometryService geometryService;
    private PlannerService plannerService;

    private LootType industrial;
    private LootType electronics;
    private Item copperWire;
    private Item battery;

    @BeforeEach
    void setUp() {
        geometryService = new GeometryService();
        plannerService = new PlannerService(gameMapRepository, mapMarkerRepository, enemyService, targetResolutionService, geometryService);

        industrial = new LootType();
        industrial.setName("Industrial");

        electronics = new LootType();
        electronics.setName("Electronics");

        copperWire = new Item();
        copperWire.setName("Copper Wire");
        copperWire.setLootType(industrial);

        battery = new Item();
        battery.setName("Battery");
        battery.setLootType(electronics);
    }

    @Test
    @DisplayName("Ongoing Items: Should populate ongoingMatchItems in AreaDto when area has matching loot type")
    void testOngoingItems_PopulatedInAreaDto() {
        // Arrange
        mockTargetItems("Copper Wire", "Industrial");
        mockOngoingItems("Battery", "Electronics");

        // Map with one area that has BOTH Industrial (Target) and Electronics (Ongoing)
        GameMap map = new GameMap();
        map.setId(1L);
        map.setName("Rich Map");
        
        Area richArea = new Area();
        richArea.setId(10L);
        richArea.setName("Warehouse");
        richArea.setMapX(0);
        richArea.setMapY(0);
        richArea.setLootAbundance(2);
        richArea.setLootTypes(Set.of(industrial, electronics));
        richArea.setCoordinates("[[0,0],[10,10]]");
        
        map.setAreas(new HashSet<>(List.of(richArea)));

        when(gameMapRepository.findAllWithAreas()).thenReturn(List.of(map));

        PlannerRequestDto request = new PlannerRequestDto(
                List.of("Copper Wire"), // Target: Industrial
                null, 
                Collections.emptyList(), // targetRecipeIds
                Collections.emptyList(), // targetContainerTypes (NEW)
                false, 
                PlannerRequestDto.RoutingProfile.PURE_SCAVENGER,
                List.of("Battery")      // Ongoing: Electronics
        );

        // Act
        List<PlannerResponseDto> response = plannerService.generateRoute(request);

        // Assert
        assertEquals(1, response.size());
        PlannerResponseDto mapResponse = response.get(0);
        assertEquals(1, mapResponse.path().size());
        
        WaypointDto waypointDto = mapResponse.path().get(0);
        assertEquals("Warehouse", waypointDto.name());
        
        // Verify Ongoing Match
        assertTrue(waypointDto.ongoingMatchItems() != null, "ongoingMatchItems should not be null");
        assertTrue(waypointDto.ongoingMatchItems().contains("Battery"), "Should contain 'Battery'");
        assertEquals(1, waypointDto.ongoingMatchItems().size());
    }

    private void mockTargetItems(String itemName, String lootType) {
        TargetItemInfo info = new TargetItemInfo(
                Set.of(lootType),
                Collections.<String>emptySet(),
                Collections.<String>emptySet(),
                Map.of(lootType, List.of(itemName)),
                Collections.<String, List<String>>emptyMap()
        );
        when(targetResolutionService.resolveTargetItems(eq(List.of(itemName)))).thenReturn(info);
        
        // Mock empty recipes/ingredients calls
        when(targetResolutionService.resolveRecipes(anyList())).thenReturn(new RecipeTargetInfo(
                Collections.<String>emptySet(), Collections.<String, Set<String>>emptyMap(), Collections.<String, String>emptyMap(), Collections.<String>emptySet()));
        when(targetResolutionService.resolveTargetItems(eq(new ArrayList<>()))).thenReturn(new TargetItemInfo(
                Collections.<String>emptySet(), Collections.<String>emptySet(), Collections.<String>emptySet(), Collections.<String, List<String>>emptyMap(), Collections.<String, List<String>>emptyMap()));
        
        // Mock container resolution to empty
        when(targetResolutionService.resolveTargetContainers(anyList(), eq(null))).thenReturn(new TargetResolutionService.ContainerTargetInfo(Collections.emptyList()));
    }

    private void mockOngoingItems(String itemName, String lootType) {
        when(targetResolutionService.resolveOngoingItems(eq(List.of(itemName))))
                .thenReturn(Map.of(lootType, List.of(itemName)));
    }
}