package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.dto.PlannerRequestDto;
import com.pauloneill.arcraidersplanner.dto.PlannerResponseDto;
import com.pauloneill.arcraidersplanner.service.PlannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for raid route planning and optimization.
 * WHY: Exposes the 4 routing profiles to help players optimize loot collection
 * based on their playstyle (scavenger, PvP avoidance, exfil priority).
 *
 * @see PlannerService#generateRoute(PlannerRequestDto)
 */
@RestController
@RequestMapping("/api/planner")
@Tag(name = "Planner", description = "Raid route planning and optimization with multiple routing profiles")
public class PlannerController {

    private final PlannerService plannerService;

    public PlannerController(PlannerService plannerService) {
        this.plannerService = plannerService;
    }

    /**
     * Generates optimal raid routes based on target items and routing strategy.
     * WHY: Players need different routing modes for different objectives
     * (pure loot, PvP avoidance, efficient exfil).
     *
     * @param request Contains target items, raider key status, routing profile
     * @return Sorted list of maps with route details (score, path, exfil point)
     */
    @Operation(
            summary = "Generate optimized raid route",
            description = """
                    Generates optimized raid routes based on target items, enemy targets, and routing strategy.

                    Supports 4 routing profiles:
                    - PURE_SCAVENGER: Maximize loot area count
                    - EASY_EXFIL: Prioritize Raider Hatch proximity for quick extraction
                    - AVOID_PVP: Edge positioning + High Tier zone avoidance to minimize combat
                    - SAFE_EXFIL: Combined safety + extraction optimization

                    Returns ranked maps with:
                    - Optimized route paths using nearest-neighbor + 2-opt algorithm
                    - Calculated route scores based on selected profile
                    - Recommended exfil points
                    - Area details with loot types
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
                    description = "Invalid request parameters (e.g., no target items specified)"
            )
    })
    @PostMapping
    public ResponseEntity<List<PlannerResponseDto>> generateRoute(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Route planning request with target items, enemies, raider key status, and routing profile",
                    required = true
            )
            @RequestBody PlannerRequestDto request) {
        List<PlannerResponseDto> results = plannerService.generateRoute(request);
        return ResponseEntity.ok(results);
    }
}
