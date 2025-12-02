package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.model.*;
import com.pauloneill.arcraidersplanner.repository.ContainerTypeRepository;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import com.pauloneill.arcraidersplanner.repository.MarkerGroupRepository;
import com.pauloneill.arcraidersplanner.repository.RecipeRepository;
import com.pauloneill.arcraidersplanner.service.TargetResolutionService.ContainerTargetInfo;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TargetResolutionServiceTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private ContainerTypeRepository containerTypeRepository;
    @Mock
    private MarkerGroupRepository markerGroupRepository;

    @InjectMocks
    private TargetResolutionService targetResolutionService;

    private LootType industrial;
    private Item copperWire;
    private Item mechanicalParts;
    private Item plastic;
    private Recipe basicTool;
    private GameMap testMap;
    private ContainerType redLockerType;
    private ContainerType raiderCacheType;
    private MarkerGroup redLockerGroup1;
    private MarkerGroup redLockerGroup2;

    @BeforeEach
    void setUp() {
        industrial = new LootType();
        industrial.setName("Industrial");

        copperWire = new Item();
        copperWire.setName("Copper Wire");
        copperWire.setLootType(industrial);
        copperWire.setDroppedBy(Set.of("Sentinel"));

        mechanicalParts = new Item();
        mechanicalParts.setName("Mechanical Parts");
        mechanicalParts.setLootType(industrial);

        plastic = new Item();
        plastic.setName("Plastic");
        plastic.setLootType(new LootType() {{ setName("Plastic"); }});

        RecipeIngredient ing1 = new RecipeIngredient();
        ing1.setItem(copperWire);
        ing1.setQuantity(2);

        RecipeIngredient ing2 = new RecipeIngredient();
        ing2.setItem(plastic);
        ing2.setQuantity(1);

        basicTool = new Recipe();
        basicTool.setMetaforgeItemId("recipe_basic_tool");
        basicTool.setName("Basic Tool");
        basicTool.setIngredients(Set.of(ing1, ing2));

        testMap = new GameMap();
        testMap.setId(1L);
        testMap.setName("Test Map");

        redLockerType = new ContainerType();
        redLockerType.setId(1L);
        redLockerType.setName("Red Locker");
        redLockerType.setSubcategory("red-locker");

        raiderCacheType = new ContainerType();
        raiderCacheType.setId(2L);
        raiderCacheType.setName("Raider Cache");
        raiderCacheType.setSubcategory("raider-cache");

        redLockerGroup1 = new MarkerGroup();
        redLockerGroup1.setId(10L);
        redLockerGroup1.setGameMap(testMap);
        redLockerGroup1.setContainerType(redLockerType);

        redLockerGroup2 = new MarkerGroup();
        redLockerGroup2.setId(11L);
        redLockerGroup2.setGameMap(testMap);
        redLockerGroup2.setContainerType(redLockerType);
    }

    @Test
    @DisplayName("resolveTargetItems: Should return correct info for valid item names")
    void testResolveTargetItems_Valid() {
        when(itemRepository.findByName("Copper Wire")).thenReturn(Optional.of(copperWire));
        when(itemRepository.findByName("Mechanical Parts")).thenReturn(Optional.of(mechanicalParts));

        List<String> itemNames = List.of("Copper Wire", "Mechanical Parts");
        TargetItemInfo info = targetResolutionService.resolveTargetItems(itemNames);

        assertEquals(1, info.targetLootTypes().size());
        assertTrue(info.targetLootTypes().contains("Industrial"));
        assertTrue(info.targetDroppedByEnemies().contains("Sentinel"));
        assertEquals(2, info.lootTypeToItemNames().get("Industrial").size());
        assertTrue(info.enemyTypeToItemNames().get("Sentinel").contains("Copper Wire"));
    }

    @Test
    @DisplayName("resolveTargetItems: Should handle empty or null item names")
    void testResolveTargetItems_EmptyOrNull() {
        TargetItemInfo info = targetResolutionService.resolveTargetItems(Collections.emptyList());
        assertTrue(info.targetLootTypes().isEmpty());
        assertTrue(info.targetDroppedByEnemies().isEmpty());

        info = targetResolutionService.resolveTargetItems(null);
        assertTrue(info.targetLootTypes().isEmpty());
        assertTrue(info.targetDroppedByEnemies().isEmpty());
    }

    @Test
    @DisplayName("resolveRecipes: Should return correct info for valid recipe IDs")
    void testResolveRecipes_Valid() {
        when(recipeRepository.findByMetaforgeItemId("recipe_basic_tool")).thenReturn(Optional.of(basicTool));
        lenient().when(itemRepository.findByName("Copper Wire")).thenReturn(Optional.of(copperWire)); // For ingredients
        lenient().when(itemRepository.findByName("Plastic")).thenReturn(Optional.of(plastic)); // For ingredients

        List<String> recipeIds = List.of("recipe_basic_tool");
        RecipeTargetInfo info = targetResolutionService.resolveRecipes(recipeIds);

        assertEquals(1, info.recipeIds().size());
        assertTrue(info.recipeToIngredientNames().get("recipe_basic_tool").contains("Copper Wire"));
        assertTrue(info.allIngredientNames().contains("Plastic"));
        assertEquals("Basic Tool", info.recipeToDisplayName().get("recipe_basic_tool"));
    }

    @Test
    @DisplayName("resolveRecipes: Should handle empty or null recipe IDs")
    void testResolveRecipes_EmptyOrNull() {
        RecipeTargetInfo info = targetResolutionService.resolveRecipes(Collections.emptyList());
        assertTrue(info.recipeIds().isEmpty());

        info = targetResolutionService.resolveRecipes(null);
        assertTrue(info.recipeIds().isEmpty());
    }

    @Test
    @DisplayName("resolveOngoingItems: Should return correct loot type map for ongoing items")
    void testResolveOngoingItems() {
        when(itemRepository.findByName("Copper Wire")).thenReturn(Optional.of(copperWire));
        when(itemRepository.findByName("Mechanical Parts")).thenReturn(Optional.of(mechanicalParts));

        List<String> ongoingItems = List.of("Copper Wire", "Mechanical Parts");
        Map<String, List<String>> result = targetResolutionService.resolveOngoingItems(ongoingItems);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("Industrial"));
        assertEquals(2, result.get("Industrial").size());
        assertTrue(result.get("Industrial").contains("Copper Wire"));
        assertTrue(result.get("Industrial").contains("Mechanical Parts"));
    }

    @Test
    @DisplayName("getLootTypesForItems: Should return correct loot types for a set of items")
    void testGetLootTypesForItems() {
        when(itemRepository.findByName("Copper Wire")).thenReturn(Optional.of(copperWire));
        when(itemRepository.findByName("Mechanical Parts")).thenReturn(Optional.of(mechanicalParts));
        
        Set<String> itemNames = Set.of("Copper Wire", "Mechanical Parts");
        Set<String> result = targetResolutionService.getLootTypesForItems(itemNames);

        assertEquals(1, result.size());
        assertTrue(result.contains("Industrial"));
    }

    @Test
    @DisplayName("resolveTargetContainers: Should return correct marker groups for valid subcategories and map ID")
    void testResolveTargetContainers_Valid() {
        when(containerTypeRepository.findBySubcategory("red-locker")).thenReturn(Optional.of(redLockerType));
        when(markerGroupRepository.findByGameMapIdAndContainerType(testMap.getId(), redLockerType))
                .thenReturn(List.of(redLockerGroup1, redLockerGroup2));

        List<String> subcategories = List.of("red-locker");
        ContainerTargetInfo info = targetResolutionService.resolveTargetContainers(subcategories, testMap.getId());

        assertEquals(2, info.markerGroups().size());
        assertTrue(info.markerGroups().contains(redLockerGroup1));
        assertTrue(info.markerGroups().contains(redLockerGroup2));
    }

    @Test
    @DisplayName("resolveTargetContainers: Should handle empty or null subcategories or map ID")
    void testResolveTargetContainers_EmptyOrNull() {
        ContainerTargetInfo info = targetResolutionService.resolveTargetContainers(Collections.emptyList(), testMap.getId());
        assertTrue(info.markerGroups().isEmpty());

        info = targetResolutionService.resolveTargetContainers(null, testMap.getId());
        assertTrue(info.markerGroups().isEmpty());

        info = targetResolutionService.resolveTargetContainers(List.of("red-locker"), null);
        assertTrue(info.markerGroups().isEmpty());
    }
}
