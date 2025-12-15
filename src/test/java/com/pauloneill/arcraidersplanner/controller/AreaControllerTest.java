package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.dto.AreaDto;
import com.pauloneill.arcraidersplanner.service.AreaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for AreaController
 * WHY: Validates zone highlighting endpoint returns correct areas for items
 */
@WebMvcTest(AreaController.class)
class AreaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AreaService areaService;

    @Test
    void shouldReturnAreasForValidMapAndItem() throws Exception {
        // Given
        AreaDto area1 = new AreaDto();
        area1.setId(1L);
        area1.setName("Factory Floor");
        area1.setMapName("Dam Battlegrounds");
        area1.setMapX(100);
        area1.setMapY(200);

        AreaDto area2 = new AreaDto();
        area2.setId(2L);
        area2.setName("Assembly Line");
        area2.setMapName("Dam Battlegrounds");
        area2.setMapX(150);
        area2.setMapY(250);

        List<AreaDto> areas = Arrays.asList(area1, area2);

        when(areaService.findAreasByMapAndItem("Dam Battlegrounds", "Steel Plate"))
                .thenReturn(areas);

        // When & Then
        mockMvc.perform(get("/api/areas/by-map-and-item")
                        .param("mapName", "Dam Battlegrounds")
                        .param("itemName", "Steel Plate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Factory Floor")))
                .andExpect(jsonPath("$[0].mapName", is("Dam Battlegrounds")))
                .andExpect(jsonPath("$[0].mapX", is(100)))
                .andExpect(jsonPath("$[0].mapY", is(200)))
                .andExpect(jsonPath("$[1].name", is("Assembly Line")));
    }

    @Test
    void shouldReturnEmptyListWhenItemHasNoLootType() throws Exception {
        // Given - item exists but has no loot type
        when(areaService.findAreasByMapAndItem("Dam Battlegrounds", "Quest Item"))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/areas/by-map-and-item")
                        .param("mapName", "Dam Battlegrounds")
                        .param("itemName", "Quest Item"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturn404WhenMapNotFound() throws Exception {
        // Given
        when(areaService.findAreasByMapAndItem("Invalid Map", "Steel Plate"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Map not found: Invalid Map"));

        // When & Then
        mockMvc.perform(get("/api/areas/by-map-and-item")
                        .param("mapName", "Invalid Map")
                        .param("itemName", "Steel Plate"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenItemNotFound() throws Exception {
        // Given
        when(areaService.findAreasByMapAndItem("Dam Battlegrounds", "Nonexistent Item"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: Nonexistent Item"));

        // When & Then
        mockMvc.perform(get("/api/areas/by-map-and-item")
                        .param("mapName", "Dam Battlegrounds")
                        .param("itemName", "Nonexistent Item"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldHandleMissingParameters() throws Exception {
        // When & Then - missing itemName
        mockMvc.perform(get("/api/areas/by-map-and-item")
                        .param("mapName", "Dam Battlegrounds"))
                .andExpect(status().isBadRequest());

        // When & Then - missing mapName
        mockMvc.perform(get("/api/areas/by-map-and-item")
                        .param("itemName", "Steel Plate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleMultipleAreasOnSameMap() throws Exception {
        // Given - same item found in 5 different areas
        List<AreaDto> manyAreas = Arrays.asList(
                createArea(1L, "Area 1", 100, 100),
                createArea(2L, "Area 2", 200, 200),
                createArea(3L, "Area 3", 300, 300),
                createArea(4L, "Area 4", 400, 400),
                createArea(5L, "Area 5", 500, 500)
        );

        when(areaService.findAreasByMapAndItem(anyString(), anyString()))
                .thenReturn(manyAreas);

        // When & Then
        mockMvc.perform(get("/api/areas/by-map-and-item")
                        .param("mapName", "Dam Battlegrounds")
                        .param("itemName", "Common Item"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    private AreaDto createArea(Long id, String name, int x, int y) {
        AreaDto area = new AreaDto();
        area.setId(id);
        area.setName(name);
        area.setMapName("Dam Battlegrounds");
        area.setMapX(x);
        area.setMapY(y);
        return area;
    }
}
