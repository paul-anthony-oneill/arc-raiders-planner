package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.dto.AreaDto;
import com.pauloneill.arcraidersplanner.service.AreaService;
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
 * REST controller for loot area queries.
 * WHY: Enables zone highlighting in tactical planner when items are selected,
 * helping players visualize where specific items can be found on the map.
 */
@RestController
@RequestMapping("/api/areas")
@Tag(name = "Areas", description = "Loot area and zone endpoints")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    /**
     * Get all areas on a map that contain a specific item.
     * WHY: Enables zone highlighting in tactical planner when item selected,
     * providing immediate visual feedback on where to find the item.
     *
     * @param mapName The map name (e.g., "Dam Battlegrounds")
     * @param itemName The item to search for
     * @return List of areas containing this item's loot type
     */
    @Operation(
            summary = "Get areas by map and item",
            description = "Returns all loot areas on a map that contain the specified item's loot type"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Areas retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AreaDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Map or item not found"
            )
    })
    @GetMapping("/by-map-and-item")
    public List<AreaDto> getAreasByMapAndItem(
            @Parameter(description = "Map name (e.g., 'Dam Battlegrounds')", required = true)
            @RequestParam String mapName,
            @Parameter(description = "Item name to search for", required = true)
            @RequestParam String itemName
    ) {
        return areaService.findAreasByMapAndItem(mapName, itemName);
    }
}
