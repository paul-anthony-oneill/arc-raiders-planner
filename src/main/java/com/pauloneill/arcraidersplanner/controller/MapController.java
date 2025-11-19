package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.dto.AreaDto;
import com.pauloneill.arcraidersplanner.dto.GameMapDto;
import com.pauloneill.arcraidersplanner.model.Area;
import com.pauloneill.arcraidersplanner.model.GameMap;
import com.pauloneill.arcraidersplanner.model.LootType;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/maps")
public class MapController {

    private final GameMapRepository mapRepository;

    public MapController(GameMapRepository mapRepository) {
        this.mapRepository = mapRepository;
    }

    /**
     * Endpoint: GET /api/maps/{name}/data
     * Fetches map details and all associated Area coordinates for visualization.
     * 
     * @param name The name of the map (e.g., "Dam Battlegrounds")
     */
    @GetMapping("/{name}/data")
    public ResponseEntity<GameMapDto> getMapData(@PathVariable String name) {
        return mapRepository.findByNameWithAreas(name)
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private GameMapDto convertToDto(GameMap map) {
        GameMapDto dto = new GameMapDto();
        dto.setId(map.getId());
        dto.setName(map.getName());
        dto.setDescription(map.getDescription());
        dto.setImageUrl(map.getImageUrl());

        if (map.getAreas() != null) {
            dto.setAreas(map.getAreas().stream()
                    .map(this::convertAreaToDto)
                    .collect(Collectors.toSet()));
        }
        return dto;
    }

    private AreaDto convertAreaToDto(Area area) {
        AreaDto dto = new AreaDto();
        dto.setId(area.getId());
        dto.setName(area.getName());
        dto.setMapX(area.getMapX());
        dto.setMapY(area.getMapY());
        if (area.getCoordinates() != null) {
            dto.setCoordinates(area.getCoordinates());
        }
        if (area.getLootTypes() != null) {
            dto.setLootTypes(area.getLootTypes().stream()
                    .map(LootType::getName)
                    .collect(Collectors.toSet()));
        }
        return dto;
    }
}