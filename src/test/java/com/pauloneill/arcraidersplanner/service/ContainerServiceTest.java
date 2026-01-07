package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.ContainerTypeDto;
import com.pauloneill.arcraidersplanner.dto.MarkerGroupDto;
import com.pauloneill.arcraidersplanner.model.ContainerType;
import com.pauloneill.arcraidersplanner.model.GameMap;
import com.pauloneill.arcraidersplanner.model.MapMarker;
import com.pauloneill.arcraidersplanner.model.MarkerGroup;
import com.pauloneill.arcraidersplanner.repository.ContainerTypeRepository;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import com.pauloneill.arcraidersplanner.repository.MarkerGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContainerServiceTest {

    @Mock
    private ContainerTypeRepository containerTypeRepository;
    @Mock
    private MarkerGroupRepository markerGroupRepository;
    @Mock
    private MapMarkerRepository mapMarkerRepository;
    @Spy
    private DtoMapper dtoMapper;

    @InjectMocks
    private ContainerService containerService;

    private ContainerType redLocker;
    private GameMap testMap;
    private MarkerGroup markerGroup;

    @BeforeEach
    void setUp() {
        redLocker = new ContainerType();
        redLocker.setId(1L);
        redLocker.setName("Red Locker");
        redLocker.setSubcategory("red-locker");

        testMap = new GameMap();
        testMap.setId(1L);
        testMap.setName("Test Map");

        markerGroup = new MarkerGroup();
        markerGroup.setId(100L);
        markerGroup.setName("Group 1");
        markerGroup.setGameMap(testMap);
        markerGroup.setContainerType(redLocker);
        markerGroup.setCenterLat(0.0);
        markerGroup.setCenterLng(0.0);
        markerGroup.setMarkerCount(5);
        markerGroup.setRadius(10.0);
    }

    @Test
    void getAllContainerTypes() {
        when(containerTypeRepository.findAll()).thenReturn(List.of(redLocker));

        List<ContainerTypeDto> result = containerService.getAllContainerTypes();

        assertEquals(1, result.size());
        assertEquals("Red Locker", result.get(0).name());
    }

    @Test
    void searchContainerTypes() {
        when(containerTypeRepository.findAll()).thenReturn(List.of(redLocker));

        List<ContainerTypeDto> result = containerService.searchContainerTypes("locker");

        assertEquals(1, result.size());
        assertEquals("Red Locker", result.get(0).name());
    }

    @Test
    void getMarkerGroups_All() {
        when(markerGroupRepository.findByGameMapId(1L)).thenReturn(List.of(markerGroup));

        List<MarkerGroupDto> result = containerService.getMarkerGroups("1", null);

        assertEquals(1, result.size());
        assertEquals("Group 1", result.get(0).name());
    }

    @Test
    void getMarkerGroups_Filtered() {
        when(containerTypeRepository.findBySubcategory("red-locker")).thenReturn(Optional.of(redLocker));
        when(markerGroupRepository.findByGameMapIdAndContainerType(1L, redLocker)).thenReturn(List.of(markerGroup));

        List<MarkerGroupDto> result = containerService.getMarkerGroups("1", "red-locker");

        assertEquals(1, result.size());
        assertEquals("Group 1", result.get(0).name());
    }

    @Test
    void getGroupMarkers() {
        MapMarker marker = new MapMarker();
        marker.setId("m1");
        markerGroup.setMarkers(List.of(marker));

        when(markerGroupRepository.findById(100L)).thenReturn(Optional.of(markerGroup));

        List<MapMarker> result = containerService.getGroupMarkers(100L);

        assertEquals(1, result.size());
        assertEquals("m1", result.get(0).getId());
    }
}
