package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.AreaDto;
import com.pauloneill.arcraidersplanner.model.Area;
import com.pauloneill.arcraidersplanner.model.GameMap;
import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import com.pauloneill.arcraidersplanner.repository.MapAreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for loot area operations.
 * WHY: Provides zone highlighting functionality for the tactical planner,
 * helping players visualize where items can be found on maps.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AreaService {

    private final MapAreaRepository areaRepository;
    private final GameMapRepository gameMapRepository;
    private final ItemRepository itemRepository;
    private final DtoMapper dtoMapper;

    /**
     * Find all areas on a map that contain a specific item's loot type.
     * WHY: Used for zone highlighting when user selects an item in the tactical planner.
     *
     * @param mapName The map name (e.g., "Dam Battlegrounds")
     * @param itemName The item name to find zones for
     * @return List of areas that contain this item's loot type, empty if item has no loot type
     * @throws ResponseStatusException 404 if map or item not found
     */
    public List<AreaDto> findAreasByMapAndItem(String mapName, String itemName) {
        // Validate map exists
        GameMap map = gameMapRepository.findByName(mapName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Map not found: " + mapName
                ));

        // Validate item exists
        Item item = itemRepository.findByName(itemName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Item not found: " + itemName
                ));

        // If item has no loot type, return empty list
        if (item.getLootType() == null) {
            return Collections.emptyList();
        }

        // Find areas by map and loot type
        String lootTypeName = item.getLootType().getName();
        List<Area> areas = areaRepository.findByMapAndLootType(map.getId(), lootTypeName);

        // Map to DTOs and include map name for frontend
        return areas.stream()
                .map(area -> {
                    AreaDto dto = dtoMapper.toDto(area);
                    dto.setMapName(mapName);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
