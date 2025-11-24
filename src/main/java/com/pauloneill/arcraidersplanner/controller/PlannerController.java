package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.dto.PlannerRequestDto;
import com.pauloneill.arcraidersplanner.dto.PlannerResponseDto;
import com.pauloneill.arcraidersplanner.service.PlannerService;
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
    @PostMapping
    public ResponseEntity<List<PlannerResponseDto>> generateRoute(
            @RequestBody PlannerRequestDto request) {
        List<PlannerResponseDto> results = plannerService.generateRoute(request);
        return ResponseEntity.ok(results);
    }
}
