package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.dto.ContainerTypeDto;
import com.pauloneill.arcraidersplanner.dto.MarkerGroupDto;
import com.pauloneill.arcraidersplanner.model.MapMarker;
import com.pauloneill.arcraidersplanner.service.ContainerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing container types and marker groups.
 * WHY: Exposes container targeting functionality to the frontend.
 */
@RestController
@RequestMapping("/api/containers")
@Tag(name = "Containers", description = "Endpoints for container types and spawn zones")
@CrossOrigin(origins = "http://localhost:5173")
public class ContainerController {

    private final ContainerService containerService;

    public ContainerController(ContainerService containerService) {
        this.containerService = containerService;
    }

    /**
     * Get all available container types.
     */
    @GetMapping
    @Operation(summary = "Get all container types", description = "Retrieve a list of all targetable container types")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Container types retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ContainerTypeDto.class)))
    })
    public List<ContainerTypeDto> getAllContainerTypes() {
        return containerService.getAllContainerTypes();
    }

    /**
     * Search container types by name.
     */
    @GetMapping("/search")
    @Operation(summary = "Search container types", description = "Search for container types by name or subcategory")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ContainerTypeDto.class)))
    })
    public List<ContainerTypeDto> searchContainers(
            @Parameter(description = "Search query", required = true)
            @RequestParam String query) {
        return containerService.searchContainerTypes(query);
    }

    /**
     * Get all marker groups for a specific map.
     */
    @GetMapping("/groups")
    @Operation(summary = "Get marker groups", description = "Retrieve clustered marker groups for a specific map, optionally filtered by container type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marker groups retrieved successfully",
                    content = @Content(schema = @Schema(implementation = MarkerGroupDto.class)))
    })
    public List<MarkerGroupDto> getMarkerGroups(
            @Parameter(description = "ID of the game map", required = true)
            @RequestParam String mapId,
            @Parameter(description = "Optional container type subcategory to filter by")
            @RequestParam(required = false) String containerType) {
        return containerService.getMarkerGroups(mapId, containerType);
    }

    /**
     * Get individual markers in a group.
     */
    @GetMapping("/groups/{groupId}/markers")
    @Operation(summary = "Get group markers", description = "Retrieve individual markers belonging to a specific group")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Group markers retrieved successfully",
                    content = @Content(schema = @Schema(implementation = MapMarker.class)))
    })
    public List<MapMarker> getGroupMarkers(
            @Parameter(description = "ID of the marker group", required = true)
            @PathVariable Long groupId) {
        return containerService.getGroupMarkers(groupId);
    }
}
