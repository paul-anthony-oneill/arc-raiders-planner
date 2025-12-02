package com.pauloneill.arcraidersplanner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pauloneill.arcraidersplanner.dto.ContainerTypeDto;
import com.pauloneill.arcraidersplanner.dto.MarkerGroupDto;
import com.pauloneill.arcraidersplanner.model.MapMarker;
import com.pauloneill.arcraidersplanner.service.ContainerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContainerController.class)
class ContainerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContainerService containerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllContainerTypes() throws Exception {
        ContainerTypeDto dto = new ContainerTypeDto(1L, "Red Locker", "red-locker", "Desc", null);
        when(containerService.getAllContainerTypes()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/containers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Red Locker"));
    }

    @Test
    void searchContainers() throws Exception {
        ContainerTypeDto dto = new ContainerTypeDto(1L, "Red Locker", "red-locker", "Desc", null);
        when(containerService.searchContainerTypes("red")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/containers/search").param("query", "red"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Red Locker"));
    }

    @Test
    void getMarkerGroups() throws Exception {
        ContainerTypeDto ctDto = new ContainerTypeDto(1L, "Red Locker", "red-locker", "Desc", null);
        MarkerGroupDto dto = new MarkerGroupDto(100L, "Group 1", 1L, "Map 1", ctDto, 0.0, 0.0, 5, 10.0, List.of());
        
        when(containerService.getMarkerGroups("1", null)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/containers/groups").param("mapId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Group 1"));
    }

    @Test
    void getGroupMarkers() throws Exception {
        MapMarker marker = new MapMarker();
        marker.setId("m1");
        marker.setLat(0.0);
        marker.setLng(0.0);
        
        when(containerService.getGroupMarkers(100L)).thenReturn(List.of(marker));

        mockMvc.perform(get("/api/containers/groups/100/markers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("m1"));
    }
}
