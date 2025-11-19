package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.model.GameMap;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * @param name The name of the map (e.g., "Dam Battlegrounds")
     */
    @GetMapping("/{name}/data")
    public ResponseEntity<GameMap> getMapData(@PathVariable String name) {
        return mapRepository.findByNameWithAreas(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}