package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.MapRecommendationDto;
import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class PlannerService {

    private final ItemRepository itemRepository;
    private final GameMapRepository gameMapRepository;

    public PlannerService(ItemRepository itemRepository, GameMapRepository gameMapRepository) {
        this.itemRepository = itemRepository;
        this.gameMapRepository = gameMapRepository;
    }

    public List<MapRecommendationDto> recommendMapsByItemName(String itemName) {
        Optional<Item> itemOptional = itemRepository.findByName(itemName);

        if (itemOptional.isEmpty()) {
            return Collections.emptyList();
        }

        Item item = itemOptional.get();

        if (item.getLootType() == null || item.getLootType().getName() == null) {
            return Collections.emptyList();
        }

        String requiredLootArea = item.getLootType().getName();

        return gameMapRepository.findMapsByLootAreaCount(requiredLootArea);
    }
}