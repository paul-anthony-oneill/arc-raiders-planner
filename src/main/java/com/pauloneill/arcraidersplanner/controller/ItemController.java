package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.dto.ItemDto;
import com.pauloneill.arcraidersplanner.dto.PlannerRequestDto;
import com.pauloneill.arcraidersplanner.dto.PlannerResponseDto;
import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.service.ItemService;
import com.pauloneill.arcraidersplanner.service.PlannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for item search and loot planning.
 * WHY: Provides item catalog access and route planning functionality
 * to help players optimize their raid objectives.
 */
@RestController
@RequestMapping("/api/items")
@Tag(name = "Items", description = "Item search and loot planning endpoints")
public class ItemController {

    private final PlannerService plannerService;
    private final ItemService itemService;

    public ItemController(PlannerService plannerService, ItemService itemService) {
        this.plannerService = plannerService;
        this.itemService = itemService;
    }

    /**
     * Search items by name or retrieve all items.
     * WHY: Players need to browse the item catalog to select targets for their raid
     *
     * @param search Optional case-insensitive search term
     * @return List of matching items with metadata (rarity, value, loot type)
     */
    @Operation(
            summary = "Search items",
            description = "Search for items by name (case-insensitive) or retrieve all items if no search term provided"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Items retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ItemDto.class))
            )
    })
    @GetMapping
    public List<ItemDto> searchItems(
            @Parameter(description = "Optional case-insensitive search term for item name")
            @RequestParam(required = false) String search) {

        List<Item> items;
        if (search != null && !search.isBlank()) {
            items = itemService.searchItems(search);
        } else {
            items = itemService.getAllItems();
        }

        return items.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Backward-compatible endpoint for legacy UI.
     * WHY: Maintains compatibility with older frontend versions that use simple single-item queries
     *
     * @param itemName Name of the target item
     * @return Map recommendations sorted by relevance, using PURE_SCAVENGER routing
     */
    @Operation(
            summary = "Get map recommendation for a single item (Legacy)",
            description = "Returns map recommendations for a single item using PURE_SCAVENGER routing profile. " +
                    "For advanced multi-item planning, use POST /api/items/plan instead."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Map recommendations generated successfully",
                    content = @Content(schema = @Schema(implementation = PlannerResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid item name provided"
            )
    })
    @GetMapping("/recommendation")
    public List<PlannerResponseDto> getRecommendation(
            @Parameter(description = "Name of the target item (case-insensitive)", required = true)
            @RequestParam String itemName) {
        PlannerRequestDto request = new PlannerRequestDto(
                List.of(itemName),
                null,
                false,
                PlannerRequestDto.RoutingProfile.PURE_SCAVENGER
        );
        return plannerService.generateRoute(request);
    }

    /**
     * Advanced route planning with multiple items, enemy targeting, and routing profiles.
     * WHY: Players need flexible route optimization supporting multiple objectives,
     * enemy hunting, and different playstyles (PvP avoidance, efficient exfil, etc.)
     *
     * @param request Planning parameters including target items, enemies, raider key, and routing profile
     * @return Ranked maps with optimized routes, exfil points, and detailed scoring
     */
    @Operation(
            summary = "Generate optimized raid route",
            description = """
                    Generate optimized raid routes based on multiple items, target enemies, and routing strategy.

                    Supports 4 routing profiles:
                    - PURE_SCAVENGER: Maximize loot area count
                    - EASY_EXFIL: Prioritize Raider Hatch proximity
                    - AVOID_PVP: Edge positioning + High Tier zone avoidance
                    - SAFE_EXFIL: Combined safety + extraction optimization

                    Enemy targeting adds bonus scoring for routes passing near specified enemy spawns.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Routes generated successfully",
                    content = @Content(schema = @Schema(implementation = PlannerResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters"
            )
    })
    @PostMapping("/plan")
    public List<PlannerResponseDto> planRoute(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Route planning request with target items, enemies, and routing profile",
                    required = true
            )
            @RequestBody PlannerRequestDto request) {
        return plannerService.generateRoute(request);
    }

    private ItemDto convertToDto(Item item) {
        ItemDto dto = new ItemDto();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setRarity(item.getRarity());
        dto.setItemType(item.getItemType());
        dto.setIconUrl(item.getIconUrl());
        dto.setValue(item.getValue());
        dto.setWeight(item.getWeight());
        dto.setStackSize(item.getStackSize());

        if (item.getLootType() != null) {
            dto.setLootType(item.getLootType().getName());
        }
        return dto;
    }
}