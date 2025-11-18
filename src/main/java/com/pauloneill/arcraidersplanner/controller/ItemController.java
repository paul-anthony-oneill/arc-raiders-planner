package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.dto.MapRecommendationDto;
import com.pauloneill.arcraidersplanner.service.PlannerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final PlannerService plannerService;

    public ItemController(PlannerService plannerService) {
        this.plannerService = plannerService;
    }

    /**
     * Endpoint: GET /api/items/recommendation?itemName={itemName}
     * Returns a list of maps, ordered by the count of areas that match the item's required loot type.
     */
    @GetMapping("/recommendation")
    public List<MapRecommendationDto> getRecommendationByItem(
            @RequestParam String itemName) {

        return plannerService.recommendMapsByItemName(itemName);
    }
}