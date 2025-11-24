package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.MetaforgeMarkerDto;
import com.pauloneill.arcraidersplanner.model.GameMap;
import com.pauloneill.arcraidersplanner.model.MapMarker;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MapMarkerService {

    private final MapMarkerRepository mapMarkerRepository;
    private final GameMapRepository gameMapRepository;
    private final RestClient restClient;

    @Transactional(readOnly = true)
    public List<MapMarker> getMarkersForMap(Long mapId) {
        return mapMarkerRepository.findByGameMapId(mapId);
    }

    @Transactional
    public void syncMarkers() {
        System.out.println("--- STARTING MARKER SYNC ---");

        // 1. Fetch ALL markers (assuming one big list for now)
        // Check the API docs: if it's paginated, use the loop. If it's a list, use ParameterizedTypeReference<List<...>>
        List<MetaforgeMarkerDto> dtos = restClient.get()
                .uri("/markers") // Verify this endpoint!
                .retrieve()
                .body(new ParameterizedTypeReference<List<MetaforgeMarkerDto>>() {
                });

        if (dtos == null) return;

        for (MetaforgeMarkerDto dto : dtos) {
            // 2. Find the linked map (e.g. "dam")
            // We assume we stored "dam" in the 'description' field of our GameMap
            Optional<GameMap> mapOpt = gameMapRepository.findAll().stream()
                    .filter(m -> m.getDescription().equalsIgnoreCase(dto.mapId()))
                    .findFirst();

            if (mapOpt.isEmpty()) {
                // System.out.println("Skipping marker " + dto.id() + " (Unknown map: " + dto.mapId() + ")");
                continue;
            }

            // 3. Save Marker
            MapMarker marker = new MapMarker();
            marker.setId(dto.id());
            marker.setLat(dto.lat());
            marker.setLng(dto.lng());
            marker.setCategory(dto.category());
            marker.setSubcategory(dto.subcategory());
            marker.setName(dto.name());
            marker.setGameMap(mapOpt.get());

            mapMarkerRepository.save(marker);
        }
        System.out.println("Synced " + dtos.size() + " markers.");
    }
}
