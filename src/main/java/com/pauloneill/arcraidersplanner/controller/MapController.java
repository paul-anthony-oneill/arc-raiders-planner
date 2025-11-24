package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.dto.AreaDto;
import com.pauloneill.arcraidersplanner.dto.GameMapDto;
import com.pauloneill.arcraidersplanner.model.Area;
import com.pauloneill.arcraidersplanner.model.GameMap;
import com.pauloneill.arcraidersplanner.model.LootType;
import com.pauloneill.arcraidersplanner.model.MapMarker;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/maps")
public class MapController {

    private final GameMapRepository mapRepository;
    private final MapMarkerRepository mapMarkerRepository;

    public MapController(GameMapRepository mapRepository, MapMarkerRepository mapMarkerRepository) {
        this.mapRepository = mapRepository;
        this.mapMarkerRepository = mapMarkerRepository;
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

    @GetMapping
    public java.util.List<GameMap> getAllMaps() {
        return mapRepository.findAll();
    }

    @GetMapping("/{id}/markers")
    public ResponseEntity<List<MapMarker>> getMapMarkers(@PathVariable Long id) {
        return mapRepository.findById(id)
                .map(map -> {
                    // Use the helper method we defined in the Repository
                    List<MapMarker> markers = mapMarkerRepository.findByGameMapId(id);
                    return ResponseEntity.ok(markers);
                })
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
        dto.setCalibrationScaleX(map.getCalibrationScaleX());
        dto.setCalibrationScaleY(map.getCalibrationScaleY());
        dto.setCalibrationOffsetX(map.getCalibrationOffsetX());
        dto.setCalibrationOffsetY(map.getCalibrationOffsetY());
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

    /**
     * Endpoint: PUT /api/maps/{id}/calibration
     * Updates the calibration constants for a specific map.
     */
    @PutMapping("/{id}/calibration")
    public ResponseEntity<GameMap> updateCalibration(
            @PathVariable Long id,
            @RequestBody CalibrationDto dto) {

        return mapRepository.findById(id)
                .map(map -> {
                    map.setCalibrationScaleX(dto.scaleX());
                    map.setCalibrationScaleY(dto.scaleY());
                    map.setCalibrationOffsetX(dto.offsetX());
                    map.setCalibrationOffsetY(dto.offsetY());
                    return ResponseEntity.ok(mapRepository.save(map));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Simple DTO for the request body (Inner record is fine for now)
    public record CalibrationDto(
            Double scaleX, Double scaleY,
            Double offsetX, Double offsetY
    ) {
    }
}