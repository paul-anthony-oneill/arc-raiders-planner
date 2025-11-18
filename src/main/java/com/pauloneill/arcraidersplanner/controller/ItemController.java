package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.dto.MapRecommendationDto;
import com.pauloneill.arcraidersplanner.service.PlannerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final PlannerService plannerService;
    private final ItemRepository itemRepository;

    public ItemController(PlannerService plannerService, ItemRepository itemRepository) {
        this.plannerService = plannerService;
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public List<Item> searchItems(
            @RequestParam(required = false) String search) {

        if (search != null && !search.isBlank()) {
            return itemRepository.findByNameContainingIgnoreCase(search);
        }
        return itemRepository.findAll();
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