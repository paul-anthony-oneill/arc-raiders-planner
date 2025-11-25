package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.MetaforgeItemDataResponse;
import com.pauloneill.arcraidersplanner.dto.MetaforgeItemDto;
import com.pauloneill.arcraidersplanner.dto.MetaforgeMapDataResponse;
import com.pauloneill.arcraidersplanner.dto.MetaforgeMarkerDto;
import com.pauloneill.arcraidersplanner.dto.MetaforgeQuestDataResponse;
import com.pauloneill.arcraidersplanner.dto.MetaforgeQuestDto;
import com.pauloneill.arcraidersplanner.model.GameMap;
import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.model.LootType;
import com.pauloneill.arcraidersplanner.model.MapMarker;
import com.pauloneill.arcraidersplanner.model.Quest;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import com.pauloneill.arcraidersplanner.repository.LootAreaRepository;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import com.pauloneill.arcraidersplanner.repository.QuestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class MetaforgeSyncService {

    private final RestClient restClient;
    private final ItemRepository itemRepository;
    private final LootAreaRepository lootAreaRepository;
    private final MapMarkerRepository markerRepository;
    private final GameMapRepository gameMapRepository;
    private final CoordinateCalibrationService calibrationService;
    private final QuestRepository questRepository;

    @Value("${metaforge.api.url}")
    private String metaforgeApiUrl;

    public MetaforgeSyncService(RestClient restClient, ItemRepository itemRepository,
            LootAreaRepository lootAreaRepository, MapMarkerRepository markerRepository,
            GameMapRepository gameMapRepository, CoordinateCalibrationService calibrationService,
            QuestRepository questRepository) {
        this.restClient = restClient;
        this.itemRepository = itemRepository;
        this.lootAreaRepository = lootAreaRepository;
        this.markerRepository = markerRepository;
        this.gameMapRepository = gameMapRepository;
        this.calibrationService = calibrationService;
        this.questRepository = questRepository;
    }

    @Transactional
    public void syncItems() {
        int currentPage = 1;
        int totalPages = 1;
        int totalItemsSynced = 0;

        // Cache existing LootTypes to avoid repeated DB lookups in loop
        Map<String, LootType> lootTypeCache = new HashMap<>();
        lootAreaRepository.findAll().forEach(lt -> lootTypeCache.put(lt.getName(), lt));

        do {
            String uri = "/arc-raiders/items?page=" + currentPage;
            log.info("Fetching items from URI: {}{}", metaforgeApiUrl, uri);

            var response = restClient.get()
                    .uri(metaforgeApiUrl + uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<MetaforgeItemDataResponse<MetaforgeItemDto>>() {
                    });

            if (response == null || response.data() == null) {
                log.error("API returned null response for page {}", currentPage);
                break;
            }

            totalPages = response.pagination().totalPages();
            List<MetaforgeItemDto> externalItems = response.data();

            // PROCESS
            for (MetaforgeItemDto dto : externalItems) {
                String areaName = dto.lootAreaName();
                LootType lootType = null;

                if (areaName != null && !areaName.isBlank()) {
                    lootType = lootTypeCache.computeIfAbsent(areaName, name -> {
                        LootType newArea = new LootType();
                        newArea.setName(name);
                        return lootAreaRepository.save(newArea);
                    });
                }

                // Create/Update the Item Entity
                Optional<Item> existingItem = itemRepository.findByName(dto.name());
                Item itemToSave = getItemToSave(dto, existingItem, lootType);

                itemRepository.save(itemToSave);
                totalItemsSynced++;
            }

            currentPage++;

        } while (currentPage <= totalPages);

        log.info("Successfully synced {} items across {} pages.", totalItemsSynced, totalPages);
    }

    private Item getItemToSave(MetaforgeItemDto dto, Optional<Item> existingItem, LootType lootType) {
        Item itemToSave = existingItem.orElse(new Item());

        itemToSave.setName(dto.name());
        itemToSave.setDescription(dto.description());
        itemToSave.setRarity(dto.rarity());
        itemToSave.setItemType(dto.itemType());
        itemToSave.setIconUrl(dto.icon());
        itemToSave.setValue(dto.value());
        itemToSave.setLootType(lootType);

        if (dto.stats() != null) {
            itemToSave.setWeight(dto.stats().weight());
            itemToSave.setStackSize(dto.stats().stackSize());
        }
        return itemToSave;
    }

    @Transactional
    public void syncMarkers() {
        log.info("--- STARTING MARKER SYNC ---");

        List<GameMap> maps = gameMapRepository.findAll();

        for (GameMap map : maps) {
            String mapApiCode = map.getDescription(); // e.g. "dam"
            log.info("Fetching markers for map: {}...", map.getName());

            try {
                var response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/game-map-data")
                                .queryParam("tableID", "arc_map_data")
                                .queryParam("mapID", mapApiCode)
                                .build())
                        .retrieve()
                        .body(MetaforgeMapDataResponse.class);

                // CHANGE 2: Extract from .allData()
                if (response == null || response.allData() == null || response.allData().isEmpty()) {
                    System.out.println("No markers found for " + map.getName());
                    continue;
                }

                List<MetaforgeMarkerDto> dtos = response.allData();

                System.out.println("Found " + dtos.size() + " markers for " + map.getName());

                for (MetaforgeMarkerDto dto : dtos) {
                    if (markerRepository.existsById(dto.id())) {
                        continue;
                    }

                    // Calibrate coordinates before storing
                    double[] calibrated = calibrationService.calibrateCoordinates(
                        dto.lat(),
                        dto.lng(),
                        map
                    );

                    MapMarker marker = new MapMarker();
                    marker.setId(dto.id());
                    marker.setLat(calibrated[0]);  // Store calibrated Y
                    marker.setLng(calibrated[1]);  // Store calibrated X
                    marker.setCategory(dto.category());
                    marker.setSubcategory(dto.subcategory());
                    marker.setName(dto.name());
                    marker.setGameMap(map);

                    markerRepository.save(marker);
                }
            } catch (Exception e) {
                log.error("Failed to sync markers for {}: {}", map.getName(), e.getMessage());
            }
        }
        log.info("--- MARKER SYNC COMPLETE ---");
    }

    @Transactional
    public void syncQuests() {
        log.info("--- STARTING QUEST SYNC ---");
        int currentPage = 1;
        int totalPages = 1;
        int totalQuestsSynced = 0;

        do {
            String uri = "/arc-raiders/quests?page=" + currentPage;
            log.info("Fetching quests from URI: {}{}", metaforgeApiUrl, uri);

            var response = restClient.get()
                    .uri(metaforgeApiUrl + uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<MetaforgeQuestDataResponse<MetaforgeQuestDto>>() {
                    });

            if (response == null || response.data() == null) {
                log.error("API returned null response for page {}", currentPage);
                break;
            }

            totalPages = response.pagination().totalPages();
            List<MetaforgeQuestDto> externalQuests = response.data();

            for (MetaforgeQuestDto dto : externalQuests) {
                Optional<Quest> existingQuest = questRepository.findById(dto.id());
                Quest questToSave = existingQuest.orElse(new Quest());

                questToSave.setId(dto.id());
                questToSave.setName(dto.name());

                List<MapMarker> markers = markerRepository.findByCategoryAndSubcategory("quests", dto.id());
                questToSave.setMarkers(new java.util.HashSet<>(markers));

                questRepository.save(questToSave);
                totalQuestsSynced++;
            }

            currentPage++;

        } while (currentPage <= totalPages);

        log.info("Successfully synced {} quests across {} pages.", totalQuestsSynced, totalPages);
        log.info("--- QUEST SYNC COMPLETE ---");
    }
}