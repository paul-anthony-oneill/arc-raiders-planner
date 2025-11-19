package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.MetaforgeItemDto;
import com.pauloneill.arcraidersplanner.dto.MetaforgeResponse;
import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.model.LootType;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import com.pauloneill.arcraidersplanner.repository.LootAreaRepository;
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

    @Value("${metaforge.api.url}")
    private String metaforgeApiUrl;

    public MetaforgeSyncService(RestClient restClient, ItemRepository itemRepository,
                                LootAreaRepository lootAreaRepository) {
        this.restClient = restClient;
        this.itemRepository = itemRepository;
        this.lootAreaRepository = lootAreaRepository;
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
            String uri = "/items?page=" + currentPage;
            log.info("Fetching items from URI: {}{}", metaforgeApiUrl, uri);

            var response = restClient.get()
                    .uri(metaforgeApiUrl + uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<MetaforgeResponse<MetaforgeItemDto>>() {
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
}