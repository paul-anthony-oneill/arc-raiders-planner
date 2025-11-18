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
        // FETCH from API
        var response = restClient.get()
                .uri("/items")
                .retrieve()
                .body(new ParameterizedTypeReference<MetaforgeResponse<MetaforgeItemDto>>() {}); // <--- CHANGED
        if (response == null || response.data() == null) return;

        List<MetaforgeItemDto> externalItems = response.data();

        // PROCESS
        for (MetaforgeItemDto dto : externalItems) {
            LootArea lootArea = lootAreaRepository.findByName(dto.lootAreaName())
                    .orElseGet(() -> {
                        LootArea newCat = new LootArea();
                        newCat.setName(dto.lootAreaName());
                        return lootAreaRepository.save(newCat);
                    });

            Optional<Item> existingItem = itemRepository.findByName(dto.name());

            Item itemToSave = existingItem.orElse(new Item());
            itemToSave.setName(dto.name());
            itemToSave.setRarity(dto.rarity());
            itemToSave.setLootArea(lootArea);
            itemToSave.setItemType(dto.itemType());

            itemRepository.save(itemToSave);
        }

        System.out.println("Successfully synced " + externalItems.size() + " items.");
    }
}