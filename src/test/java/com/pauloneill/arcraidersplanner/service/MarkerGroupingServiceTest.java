package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.model.ContainerType;
import com.pauloneill.arcraidersplanner.model.GameMap;
import com.pauloneill.arcraidersplanner.model.MapMarker;
import com.pauloneill.arcraidersplanner.model.MarkerGroup;
import com.pauloneill.arcraidersplanner.repository.ContainerTypeRepository;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import com.pauloneill.arcraidersplanner.repository.MarkerGroupRepository;
import com.pauloneill.arcraidersplanner.service.GeometryService.ClusterMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarkerGroupingServiceTest {

    @Mock
    private MapMarkerRepository mapMarkerRepository;
    @Mock
    private MarkerGroupRepository markerGroupRepository;
    @Mock
    private ContainerTypeRepository containerTypeRepository;
    @Mock
    private GameMapRepository gameMapRepository;
    @Mock
    private GeometryService geometryService;

    @InjectMocks
    private MarkerGroupingService markerGroupingService;

    private GameMap testMap;
    private ContainerType redLockerType;
    private ContainerType raiderCacheType;

    @BeforeEach
    void setUp() {
        testMap = new GameMap();
        testMap.setId(1L);
        testMap.setName("Test Map");

        redLockerType = new ContainerType();
        redLockerType.setId(10L);
        redLockerType.setName("Red Locker");
        redLockerType.setSubcategory("red-locker");

        raiderCacheType = new ContainerType();
        raiderCacheType.setId(11L);
        raiderCacheType.setName("Raider Cache");
        raiderCacheType.setSubcategory("raider-cache");

        // Removed all stubbings from setUp. Each test will explicitly mock what it needs.
    }

    @Test
    @DisplayName("Should clear existing marker groups and their links before re-grouping")
    void shouldClearExistingGroupsBeforeRegrouping() {
        // Arrange
        MarkerGroup oldGroup = new MarkerGroup();
        oldGroup.setId(100L);
        oldGroup.setGameMap(testMap);

        MapMarker markerInOldGroup = new MapMarker();
        markerInOldGroup.setId("m1");
        markerInOldGroup.setMarkerGroup(oldGroup);

        when(gameMapRepository.findById(1L)).thenReturn(Optional.of(testMap)); // Specific to this test
        when(containerTypeRepository.findAll()).thenReturn(List.of(redLockerType, raiderCacheType)); // Specific to this test
        when(markerGroupRepository.findByGameMapId(testMap.getId())).thenReturn(List.of(oldGroup));
        when(mapMarkerRepository.findByMarkerGroup(oldGroup)).thenReturn(List.of(markerInOldGroup));
        
        // Default mock for mapMarkerRepository.findByGameMapIdAndSubcategoryAndCategoryNot
        // This is necessary because MarkerGroupingService calls this method even if it expects empty.
        lenient().when(mapMarkerRepository.findByGameMapIdAndSubcategoryAndCategoryNot(
                eq(testMap.getId()), anyString(), eq("poi"))).thenReturn(Collections.emptyList());

        // Default mock for clustering as it will be called by the service, but no actual clusters formed
        lenient().when(geometryService.clusterMarkersByProximity(anyList(), anyDouble(), anyInt())).thenReturn(Collections.emptyMap());

        // Act
        markerGroupingService.groupMarkersByContainer(testMap.getId());

        // Assert
        verify(markerGroupRepository).findByGameMapId(testMap.getId()); // verify initial fetch
        verify(mapMarkerRepository).findByMarkerGroup(oldGroup); // verify markers in old group are fetched
        verify(mapMarkerRepository).save(argThat(m -> m.getId().equals("m1") && m.getMarkerGroup() == null && !m.getIsGrouped()));
        verify(markerGroupRepository).delete(oldGroup);
        verify(markerGroupRepository, times(0)).save(any(MarkerGroup.class)); // No new groups if no new markers
    }


    @Test
    @DisplayName("Should not create groups if no eligible markers for a container type")
    void shouldNotCreateGroupsIfNoEligibleMarkers() {
        // Arrange
        when(gameMapRepository.findById(1L)).thenReturn(Optional.of(testMap)); // Specific to this test
        when(containerTypeRepository.findAll()).thenReturn(List.of(redLockerType, raiderCacheType)); // Specific to this test
        when(markerGroupRepository.findByGameMapId(testMap.getId())).thenReturn(Collections.emptyList());
        // findByGameMapIdAndSubcategoryAndCategoryNot returns empty by default since it's not stubbed here
        // for any specific arguments, thus it will implicitly return empty list.

        // Act
        markerGroupingService.groupMarkersByContainer(testMap.getId());

        // Assert
        // Verified that findByGameMapIdAndSubcategoryAndCategoryNot is called for each container type
        verify(mapMarkerRepository, times(2)).findByGameMapIdAndSubcategoryAndCategoryNot(eq(testMap.getId()), anyString(), eq("poi"));
        verify(geometryService, never()).clusterMarkersByProximity(anyList(), anyDouble(), anyInt());
        verify(markerGroupRepository, never()).save(any(MarkerGroup.class));
        verify(mapMarkerRepository, never()).save(any(MapMarker.class));
    }

    @Test
    @DisplayName("Should create marker groups from clustered markers and link them")
    void shouldCreateAndLinkMarkerGroupsFromClusters() {
        // Arrange
        MapMarker m1 = createMarker("m1", 0, 0, redLockerType.getSubcategory());
        MapMarker m2 = createMarker("m2", 1, 1, redLockerType.getSubcategory());
        MapMarker m3 = createMarker("m3", 10, 10, redLockerType.getSubcategory()); // Standalone

        List<MapMarker> redLockerMarkers = List.of(m1, m2, m3);

        when(gameMapRepository.findById(1L)).thenReturn(Optional.of(testMap)); // Specific to this test
        when(containerTypeRepository.findAll()).thenReturn(List.of(redLockerType, raiderCacheType)); // Specific to this test
        when(markerGroupRepository.findByGameMapId(testMap.getId())).thenReturn(Collections.emptyList()); // Specific to this test

        // Mock clustering: m1, m2 form a cluster, m3 is standalone (due to minClusterSize=2)
        Map<Integer, List<MapMarker>> clusters = Map.of(
                0, List.of(m1, m2),
                1, List.of(m3)
        );
        when(mapMarkerRepository.findByGameMapIdAndSubcategoryAndCategoryNot(
                testMap.getId(), redLockerType.getSubcategory(), "poi"))
                .thenReturn(redLockerMarkers);
        when(geometryService.clusterMarkersByProximity(
                argThat(list -> list.contains(m1) && list.contains(m2) && list.contains(m3)),
                eq(50.0), // red-locker maxDistance
                eq(2)
        )).thenReturn(clusters);

        when(geometryService.calculateClusterMetrics(anyList())) // Use anyList here too
                .thenReturn(new ClusterMetrics(0.5, 0.5, 1.0, 2));

        // Use ArgumentCaptor for the saved MarkerGroup
        ArgumentCaptor<MarkerGroup> markerGroupCaptor = ArgumentCaptor.forClass(MarkerGroup.class);
        when(markerGroupRepository.save(markerGroupCaptor.capture()))
                .thenAnswer(invocation -> {
                    MarkerGroup mg = invocation.getArgument(0);
                    mg.setId(1L); // Simulate saved ID
                    mg.setGameMap(testMap);
                    mg.setContainerType(redLockerType);
                    mg.setName(markerGroupingService.generateGroupName(testMap.getName(), redLockerType, 1));
                    return mg;
                });
        
        // Act
        markerGroupingService.groupMarkersByContainer(testMap.getId());

        // Assert
        verify(markerGroupRepository, times(1)).save(any(MarkerGroup.class));
        MarkerGroup capturedMarkerGroup = markerGroupCaptor.getValue(); // Get the actual saved instance
        
        ArgumentCaptor<MapMarker> markerCaptor = ArgumentCaptor.forClass(MapMarker.class);
        verify(mapMarkerRepository, times(3)).save(markerCaptor.capture());

        List<MapMarker> savedMarkers = markerCaptor.getAllValues();
        assertEquals(3, savedMarkers.size());

        // Verify standalone marker state
        assertTrue(savedMarkers.stream().anyMatch(
                marker -> "m3".equals(marker.getId()) && !marker.getIsGrouped() && "isolated".equals(marker.getStandaloneReason()) && marker.getMarkerGroup() == null
        ));
        // Verify grouped markers state using the capturedMarkerGroup for comparison
        assertTrue(savedMarkers.stream().anyMatch(
                marker -> "m1".equals(marker.getId()) && marker.getIsGrouped() && capturedMarkerGroup.equals(marker.getMarkerGroup())
        ));
        assertTrue(savedMarkers.stream().anyMatch(
                marker -> "m2".equals(marker.getId()) && marker.getIsGrouped() && capturedMarkerGroup.equals(marker.getMarkerGroup())
        ));
    }

    @Test
    @DisplayName("Should handle different max distances for different container types")
    void shouldCalculateDifferentMaxDistances() {
        // Arrange
        MapMarker m1 = createMarker("m1", 0, 0, redLockerType.getSubcategory());
        MapMarker m4 = createMarker("m4", 100, 100, raiderCacheType.getSubcategory());
        
        when(gameMapRepository.findById(1L)).thenReturn(Optional.of(testMap));
        when(containerTypeRepository.findAll()).thenReturn(List.of(redLockerType, raiderCacheType));
        when(markerGroupRepository.findByGameMapId(testMap.getId())).thenReturn(Collections.emptyList());

        when(mapMarkerRepository.findByGameMapIdAndSubcategoryAndCategoryNot(
                eq(testMap.getId()), eq(redLockerType.getSubcategory()), eq("poi")))
                .thenReturn(List.of(m1));
        when(mapMarkerRepository.findByGameMapIdAndSubcategoryAndCategoryNot(
                eq(testMap.getId()), eq(raiderCacheType.getSubcategory()), eq("poi")))
                .thenReturn(List.of(m4));

        // Use lenient().when() for the clustering mock as its return value is not directly asserted in this test,
        // and its invocation arguments are verified separately.
        lenient().when(geometryService.clusterMarkersByProximity(anyList(), anyDouble(), anyInt())).thenReturn(Collections.emptyMap());

        lenient().when(markerGroupRepository.save(any(MarkerGroup.class))).thenReturn(new MarkerGroup()); // Prevent NPE if save called
        lenient().when(mapMarkerRepository.save(any(MapMarker.class))).thenReturn(new MapMarker()); // Prevent NPE if save called

        // Act
        markerGroupingService.groupMarkersByContainer(testMap.getId());

        // Assert
        // Verify GeometryService.clusterMarkersByProximity was called with correct maxDistance
        verify(geometryService).clusterMarkersByProximity(anyList(), eq(50.0), anyInt()); // red-locker
        verify(geometryService).clusterMarkersByProximity(anyList(), eq(100.0), anyInt()); // raider-cache
    }

    // Helper method to create a MapMarker
    private MapMarker createMarker(String id, double lng, double lat, String subcategory) {
        MapMarker marker = new MapMarker();
        marker.setId(id);
        marker.setLng(lng);
        marker.setLat(lat);
        marker.setSubcategory(subcategory);
        marker.setCategory("loot"); // Assuming container markers are "loot" category
        marker.setGameMap(testMap);
        return marker;
    }
}
