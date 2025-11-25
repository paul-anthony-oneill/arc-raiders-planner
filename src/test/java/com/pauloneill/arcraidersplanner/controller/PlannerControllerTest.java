package com.pauloneill.arcraidersplanner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pauloneill.arcraidersplanner.dto.PlannerRequestDto;
import com.pauloneill.arcraidersplanner.dto.PlannerResponseDto;
import com.pauloneill.arcraidersplanner.service.PlannerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PlannerController REST API.
 * WHY: Verify the controller properly handles HTTP requests and responses
 * for all routing profiles.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PlannerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlannerService plannerService;

    @Test
    @DisplayName("POST /api/planner - Should return route recommendations for PURE_SCAVENGER")
    void shouldReturnRouteRecommendations() throws Exception {
        // Arrange
        PlannerRequestDto request = new PlannerRequestDto(
                List.of("Copper Wire"),
                null,
                null,
                false,
                PlannerRequestDto.RoutingProfile.PURE_SCAVENGER
        );

        PlannerResponseDto mockResponse = new PlannerResponseDto(
                1L,
                "The Spaceport",
                200.0,
                Collections.emptyList(),
                null,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(plannerService.generateRoute(any(PlannerRequestDto.class)))
                .thenReturn(List.of(mockResponse));

        // Act & Assert
        mockMvc.perform(post("/api/planner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mapName").value("The Spaceport"))
                .andExpect(jsonPath("$[0].score").value(200.0))
                .andExpect(jsonPath("$[0].extractionPoint").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/planner - Should return extraction point for EASY_EXFIL")
    void shouldReturnExtractionPointForEasyExfil() throws Exception {
        // Arrange
        PlannerRequestDto request = new PlannerRequestDto(
                List.of("Copper Wire"),
                null,
                null,
                true,
                PlannerRequestDto.RoutingProfile.EASY_EXFIL
        );

        PlannerResponseDto mockResponse = new PlannerResponseDto(
                1L,
                "The Spaceport",
                150.0,
                Collections.emptyList(),
                "Raider Hatch Alpha",
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(plannerService.generateRoute(any(PlannerRequestDto.class)))
                .thenReturn(List.of(mockResponse));

        // Act & Assert
        mockMvc.perform(post("/api/planner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mapName").value("The Spaceport"))
                .andExpect(jsonPath("$[0].extractionPoint").value("Raider Hatch Alpha"));
    }

    @Test
    @DisplayName("POST /api/planner - Should handle AVOID_PVP profile")
    void shouldHandleAvoidPvPProfile() throws Exception {
        // Arrange
        PlannerRequestDto request = new PlannerRequestDto(
                List.of("Industrial Parts"),
                null,
                null,
                false,
                PlannerRequestDto.RoutingProfile.AVOID_PVP
        );

        PlannerResponseDto mockResponse = new PlannerResponseDto(
                2L,
                "Buried City",
                180.0,
                Collections.emptyList(),
                null,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(plannerService.generateRoute(any(PlannerRequestDto.class)))
                .thenReturn(List.of(mockResponse));

        // Act & Assert
        mockMvc.perform(post("/api/planner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mapName").value("Buried City"))
                .andExpect(jsonPath("$[0].score").value(180.0));
    }

    @Test
    @DisplayName("POST /api/planner - Should handle SAFE_EXFIL profile")
    void shouldHandleSafeExfilProfile() throws Exception {
        // Arrange
        PlannerRequestDto request = new PlannerRequestDto(
                List.of("Mechanical Components"),
                null,
                null,
                true,
                PlannerRequestDto.RoutingProfile.SAFE_EXFIL
        );

        PlannerResponseDto mockResponse = new PlannerResponseDto(
                3L,
                "Blue Gate",
                170.0,
                Collections.emptyList(),
                "Safe Hatch Beta",
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(plannerService.generateRoute(any(PlannerRequestDto.class)))
                .thenReturn(List.of(mockResponse));

        // Act & Assert
        mockMvc.perform(post("/api/planner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mapName").value("Blue Gate"))
                .andExpect(jsonPath("$[0].extractionPoint").value("Safe Hatch Beta"));
    }

    @Test
    @DisplayName("POST /api/planner - Should return empty list when no items match")
    void shouldReturnEmptyListWhenNoMatches() throws Exception {
        // Arrange
        PlannerRequestDto request = new PlannerRequestDto(
                List.of("Nonexistent Item"),
                null,
                null,
                false,
                PlannerRequestDto.RoutingProfile.PURE_SCAVENGER
        );

        when(plannerService.generateRoute(any(PlannerRequestDto.class)))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(post("/api/planner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("POST /api/planner - Should return multiple map results sorted by score")
    void shouldReturnMultipleMapsRankedByScore() throws Exception {
        // Arrange
        PlannerRequestDto request = new PlannerRequestDto(
                List.of("Copper Wire"),
                null,
                null,
                false,
                PlannerRequestDto.RoutingProfile.PURE_SCAVENGER
        );

        PlannerResponseDto map1 = new PlannerResponseDto(1L, "The Spaceport", 300.0, Collections.emptyList(), null, null, null, Collections.emptyList(), Collections.emptyList());
        PlannerResponseDto map2 = new PlannerResponseDto(2L, "Buried City", 200.0, Collections.emptyList(), null, null, null, Collections.emptyList(), Collections.emptyList());
        PlannerResponseDto map3 = new PlannerResponseDto(3L, "Blue Gate", 100.0, Collections.emptyList(), null, null, null, Collections.emptyList(), Collections.emptyList());

        when(plannerService.generateRoute(any(PlannerRequestDto.class)))
                .thenReturn(List.of(map1, map2, map3));

        // Act & Assert
        mockMvc.perform(post("/api/planner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].mapName").value("The Spaceport"))
                .andExpect(jsonPath("$[0].score").value(300.0))
                .andExpect(jsonPath("$[1].mapName").value("Buried City"))
                .andExpect(jsonPath("$[2].mapName").value("Blue Gate"));
    }
}