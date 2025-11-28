package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.dto.AreaDto;
import com.pauloneill.arcraidersplanner.dto.GameMapDto;
import com.pauloneill.arcraidersplanner.model.Area;
import com.pauloneill.arcraidersplanner.model.GameMap;
import com.pauloneill.arcraidersplanner.model.LootType;
import com.pauloneill.arcraidersplanner.model.MapMarker;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
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
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for game map data and visualization.
 * WHY: Provides map metadata, loot area coordinates, markers, and calibration
 * data
 * for rendering interactive map overlays in the frontend.
 */
@RestController
@RequestMapping("/api/maps")
@Tag(name = "Maps", description = "Game map data, areas, markers, and calibration endpoints")
public class MapController {

        private final GameMapRepository mapRepository;
        private final MapMarkerRepository mapMarkerRepository;

        public MapController(GameMapRepository mapRepository, MapMarkerRepository mapMarkerRepository) {
                this.mapRepository = mapRepository;
                this.mapMarkerRepository = mapMarkerRepository;
        }

        /**
         * Get comprehensive map data including areas and calibration settings.
         * WHY: Frontend needs all loot area coordinates and calibration constants
         * to render interactive overlays on map images.
         *
         * @param name The name of the map (e.g., "Dam Battlegrounds")
         * @return Map DTO with areas, loot types, and calibration data
         */
        @Operation(summary = "Get map data with areas", description = "Retrieves complete map information including all loot areas, their coordinates, "
                        +
                        "associated loot types, and calibration constants for coordinate transformation")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Map data retrieved successfully", content = @Content(schema = @Schema(implementation = GameMapDto.class))),
                        @ApiResponse(responseCode = "404", description = "Map not found")
        })
        @GetMapping("/{name}/data")
        public ResponseEntity<GameMapDto> getMapData(
                        @Parameter(description = "Name of the map (e.g., 'Dam Battlegrounds')", required = true) @PathVariable String name) {
                return mapRepository.findByNameWithAreas(name)
                                .map(this::convertToDto)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        /**
         * Get all available game maps.
         * WHY: Frontend needs the complete map catalog for dropdown selection
         *
         * @return List of all maps with basic metadata
         */
        @Operation(summary = "List all maps", description = "Retrieves all available game maps with basic information")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Maps retrieved successfully", content = @Content(schema = @Schema(implementation = GameMap.class)))
        })
        @GetMapping
        public java.util.List<GameMap> getAllMaps() {
                return mapRepository.findAll();
        }

        // /**
        // * Get all markers for a specific map.
        // * WHY: Frontend needs marker locations (ARC enemies, Raider Hatches) to
        // render
        // * points of interest on the map visualization.
        // *
        // * @param id Database ID of the map
        // * @return List of markers with types, names, and lat/lng coordinates
        // */
        // @Operation(summary = "Get map markers", description = "Retrieves all markers
        // (ARC enemy spawns, Raider Hatches, etc.) for a specific map")
        // @ApiResponses({
        // @ApiResponse(responseCode = "200", description = "Markers retrieved
        // successfully", content = @Content(schema = @Schema(implementation =
        // MapMarker.class))),
        // @ApiResponse(responseCode = "404", description = "Map not found")
        // })
        // @GetMapping("/{id}/markers")
        // public ResponseEntity<List<MapMarker>> getMapMarkers(
        // @Parameter(description = "Database ID of the map", required = true)
        // @PathVariable("id") Long mapDbId) {
        // // First, check if the map exists
        // Optional<GameMap> gameMapOptional = mapRepository.findById(mapDbId);
        // if (gameMapOptional.isEmpty()) {
        // return ResponseEntity.notFound().build();
        // }
        // // If map exists, retrieve markers
        // List<MapMarker> markers = mapMarkerRepository.findByGameMapId(mapDbId);
        // return ResponseEntity.ok(markers);
        // }

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
                dto.setId(Long.valueOf(area.getId()));
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
         * Update map calibration constants for coordinate transformation.
         * WHY: Allows fine-tuning of the coordinate conversion between lat/lng and
         * pixel coordinates
         * to ensure accurate marker placement on map images.
         *
         * @param id  Database ID of the map
         * @param dto Calibration constants (scaleX, scaleY, offsetX, offsetY)
         * @return Updated map entity
         */
        @Operation(summary = "Update map calibration", description = "Updates the calibration constants used to transform between lat/lng coordinates "
                        +
                        "and pixel coordinates for accurate marker placement on map images")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Calibration updated successfully", content = @Content(schema = @Schema(implementation = GameMap.class))),
                        @ApiResponse(responseCode = "404", description = "Map not found"),
                        @ApiResponse(responseCode = "400", description = "Invalid calibration data")
        })
        @PutMapping("/{id}/calibration")
        public ResponseEntity<GameMap> updateCalibration(
                        @Parameter(description = "Database ID of the map", required = true) @PathVariable Long id,
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Calibration constants for coordinate transformation", required = true) @RequestBody CalibrationDto dto) {

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
                        Double offsetX, Double offsetY) {
        }
}