package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.MetaforgeMapDataResponse;
import com.pauloneill.arcraidersplanner.dto.MetaforgeMarkerDto;
import com.pauloneill.arcraidersplanner.model.GameMap;
import com.pauloneill.arcraidersplanner.model.MapMarker;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class MetaforgeSyncServiceTest {

    @Autowired
    private MetaforgeSyncService syncService;

    @Autowired
    private MapMarkerRepository markerRepository;

    @Autowired
    private GameMapRepository gameMapRepository;

    @MockBean
    private RestClient restClient;

    @MockBean
    private MarkerGroupingService markerGroupingService;

    private GameMap testMap;

    private RestClient.Builder restClientBuilderMock;
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpecMock;
    private RestClient.ResponseSpec responseSpecMock;

    @BeforeEach
    void setUp() {
        testMap = new GameMap();
        testMap.setName("Test Map");
        testMap.setDescription("test-map");
        testMap.setCalibrationScaleX(1.0);
        testMap.setCalibrationScaleY(1.0);
        testMap.setCalibrationOffsetX(0.0);
        testMap.setCalibrationOffsetY(0.0);
        gameMapRepository.save(testMap);

        // Mock the RestClient's behavior
        restClientBuilderMock = mock(RestClient.Builder.class);
        requestHeadersUriSpecMock = mock(RestClient.RequestHeadersUriSpec.class);
        responseSpecMock = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(requestHeadersUriSpecMock);
        when(requestHeadersUriSpecMock.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersUriSpecMock);
        when(requestHeadersUriSpecMock.retrieve()).thenReturn(responseSpecMock);
    }

    @Test
    void testMarkersStoreCalibratedCoordinates() {
        // Mock Metaforge API response for markers
        MetaforgeMarkerDto dummyMarker = new MetaforgeMarkerDto(
                "marker-1", 100.0, 200.0, testMap.getDescription(), "arc", "sentinel", "Sentinel Prime"
        );
        MetaforgeMapDataResponse mockResponse = new MetaforgeMapDataResponse(List.of(dummyMarker));

        when(responseSpecMock.body(MetaforgeMapDataResponse.class)).thenReturn(mockResponse);

        // Run marker sync
        syncService.syncMarkers();

        // Verify markers have calibrated coordinates
        List<MapMarker> markers = markerRepository.findAll();
        assertFalse(markers.isEmpty(), "Should have synced some markers");
        assertEquals(1, markers.size());

        MapMarker syncedMarker = markers.getFirst();
        assertEquals("marker-1", syncedMarker.getId());
        assertEquals(100.0, syncedMarker.getLat());
        assertEquals(200.0, syncedMarker.getLng());
        assertEquals("arc", syncedMarker.getCategory());
        assertEquals("sentinel", syncedMarker.getSubcategory());
        assertEquals("Sentinel Prime", syncedMarker.getName());
    }

    @Test
    void testCalibratedCoordinatesDifferFromRaw() {
        // Mock Metaforge API response for markers with non-identity calibration
        testMap.setCalibrationScaleX(2.0);
        testMap.setCalibrationScaleY(0.5);
        testMap.setCalibrationOffsetX(10.0);
        testMap.setCalibrationOffsetY(-5.0);
        gameMapRepository.save(testMap); // Update the map with new calibration

        MetaforgeMarkerDto rawMarker = new MetaforgeMarkerDto(
                "marker-calibrated", 50.0, 100.0, testMap.getDescription(), "arc", "brute", "Brute Alpha"
        );
        MetaforgeMapDataResponse mockResponse = new MetaforgeMapDataResponse(List.of(rawMarker));

        when(responseSpecMock.body(MetaforgeMapDataResponse.class)).thenReturn(mockResponse);

        syncService.syncMarkers();

        List<MapMarker> markers = markerRepository.findAll();
        assertFalse(markers.isEmpty(), "Should have synced some markers with calibration");
        MapMarker calibratedMarker = markers.getFirst();

        // Expected calibrated values:
        // calibratedX = (rawX * scaleX) + offsetX = (100.0 * 2.0) + 10.0 = 210.0
        // calibratedY = (rawY * scaleY) + offsetY = (50.0 * 0.5) - 5.0 = 25.0 - 5.0 = 20.0
        assertEquals(20.0, calibratedMarker.getLat(), 0.001); // lat is Y
        assertEquals(210.0, calibratedMarker.getLng(), 0.001); // lng is X
    }
}
