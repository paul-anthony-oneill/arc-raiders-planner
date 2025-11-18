package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.MetaforgeItemDto;
import com.pauloneill.arcraidersplanner.dto.MetaforgeResponse;
import com.pauloneill.arcraidersplanner.model.LootArea;
import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.repository.LootAreaRepository;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MetaforgeSyncService {

    private final RestClient restClient;
    private final ItemRepository itemRepository;
    private final LootAreaRepository lootAreaRepository;

    public MetaforgeSyncService(RestClient restClient, ItemRepository itemRepository, LootAreaRepository lootAreaRepository) {
        this.restClient = restClient;
        this.itemRepository = itemRepository;
        this.lootAreaRepository = lootAreaRepository;
    }

    @Transactional
    public void syncItems() {
        int currentPage = 1;
        int totalPages = 1;
        int totalItemsSynced = 0;

        do {
            String uri = "/items?page=" + currentPage;
            System.out.println("Fetching items from URI: " + uri);

            var response = restClient.get()
                    .uri("https://metaforge.app/api/arc-raiders" + uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<MetaforgeResponse<MetaforgeItemDto>>() {
                    });

            if (response == null || response.data() == null) {
                System.err.println("API returned null response for page " + currentPage);
                break;
            }

            totalPages = response.pagination().totalPages();
            List<MetaforgeItemDto> externalItems = response.data();

            // PROCESS
            for (MetaforgeItemDto dto : externalItems) {
                String areaName = dto.lootAreaName();
                LootArea lootArea = null;

                if (areaName != null && !areaName.isBlank()) {
                    lootArea = lootAreaRepository.findByName(areaName)
                            .orElseGet(() -> {
                                LootArea newArea = new LootArea();
                                newArea.setName(areaName);
                                return lootAreaRepository.save(newArea);
                            });
                }

                // Create/Update the Item Entity
                Optional<Item> existingItem = itemRepository.findByName(dto.name());
                Item itemToSave = getItemToSave(dto, existingItem, lootArea);

                itemRepository.save(itemToSave);
                totalItemsSynced++;
            }

            currentPage++;

        } while (currentPage <= totalPages);

        System.out.println("Successfully synced " + totalItemsSynced + " items across " + totalPages + " pages.");
    }

    private Item getItemToSave(MetaforgeItemDto dto, Optional<Item> existingItem, LootArea lootArea) {
        Item itemToSave = existingItem.orElse(new Item());

        itemToSave.setName(dto.name());
        itemToSave.setDescription(dto.description());
        itemToSave.setRarity(dto.rarity());
        itemToSave.setItemType(dto.itemType());
        itemToSave.setIconUrl(dto.icon());
        itemToSave.setValue(dto.value());
        itemToSave.setLootArea(lootArea);

        if (dto.stats() != null) {
            itemToSave.setWeight(dto.stats().weight());
            itemToSave.setStackSize(dto.stats().stackSize());
        }
        return itemToSave;
    }
}