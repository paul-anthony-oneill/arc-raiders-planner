package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.dto.EnemyDto;
import com.pauloneill.arcraidersplanner.service.EnemyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API endpoints for ARC enemy management.
 * WHY: Enables frontend to search and select enemies for raid planning
 */
@RestController
@RequestMapping("/api/enemies")
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
    @GetMapping
    public List<EnemyDto> searchEnemies(
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
    @GetMapping("/{id}")
    public ResponseEntity<EnemyDto> getEnemyById(@PathVariable String id) {
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
    @GetMapping("/types")
    public List<String> getEnemyTypes() {
        return enemyService.getSelectableEnemyTypes();
    }
}
