package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.dto.EnemyDto;
import com.pauloneill.arcraidersplanner.service.EnemyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API endpoints for ARC enemy management.
 * WHY: Enables frontend to search and select enemies for raid planning
 */
@RestController
@RequestMapping("/api/enemies")
@Tag(name = "Enemies", description = "ARC enemy search and catalog endpoints")
public class EnemyController {

    private final EnemyService enemyService;

    public EnemyController(EnemyService enemyService) {
        this.enemyService = enemyService;
    }

    /**
     * Search or list all ARC enemies.
     * WHY: Provides searchable enemy catalog for frontend selection
     *
     * @param search Optional search term (case-insensitive)
     * @return List of matching enemies
     */
    @Operation(
            summary = "Search ARC enemies",
            description = "Search for ARC enemies by name (case-insensitive) or retrieve all enemies if no search term provided"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Enemies retrieved successfully",
                    content = @Content(schema = @Schema(implementation = EnemyDto.class))
            )
    })
    @GetMapping
    public List<EnemyDto> searchEnemies(
            @Parameter(description = "Optional case-insensitive search term for enemy name")
            @RequestParam(required = false) String search) {

        if (search != null && !search.isBlank()) {
            return enemyService.searchByName(search);
        }
        return enemyService.getAllArcEnemies();
    }

    /**
     * Get a specific enemy by ID.
     * WHY: Allows frontend to fetch enemy details
     *
     * @param id Enemy UUID
     * @return Enemy DTO or 404
     */
    @Operation(
            summary = "Get enemy by ID",
            description = "Retrieves a specific ARC enemy by its unique UUID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Enemy found",
                    content = @Content(schema = @Schema(implementation = EnemyDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Enemy not found"
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<EnemyDto> getEnemyById(
            @Parameter(description = "Unique UUID of the enemy", required = true)
            @PathVariable String id) {
        List<EnemyDto> allEnemies = enemyService.getAllArcEnemies();
        return allEnemies.stream()
                .filter(e -> e.id().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all selectable enemy types (excludes small flying enemies).
     * WHY: Players select enemy types for routing, not specific spawns
     *
     * @return List of unique enemy types (e.g., "Sentinel", "Guardian") - excludes Wasp, Hornet, Snitch
     */
    @Operation(
            summary = "Get selectable enemy types",
            description = "Retrieves all distinct enemy types suitable for route planning. " +
                    "Excludes small flying enemies (Wasp, Hornet, Snitch) that are not primary raid targets."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Enemy types retrieved successfully",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    @GetMapping("/types")
    public List<String> getEnemyTypes() {
        return enemyService.getSelectableEnemyTypes();
    }
}
