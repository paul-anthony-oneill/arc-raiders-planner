package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.dto.ItemDto;
import com.pauloneill.arcraidersplanner.dto.PlannerRequestDto;
import com.pauloneill.arcraidersplanner.dto.PlannerResponseDto;
import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.service.ItemService;
import com.pauloneill.arcraidersplanner.service.PlannerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final PlannerService plannerService;
    private final ItemService itemService;

    public ItemController(PlannerService plannerService, ItemService itemService) {
        this.plannerService = plannerService;
        this.itemService = itemService;
    }

    @GetMapping
    public List<ItemDto> searchItems(
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
     * Endpoint: POST /api/items/plan
     * Accepts a sophisticated loadout request (items, keys, preferred mode)
     * and returns ranked maps with calculated routes.
     */
    @PostMapping("/plan")
    public List<PlannerResponseDto> planRoute(@RequestBody PlannerRequestDto request) {
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