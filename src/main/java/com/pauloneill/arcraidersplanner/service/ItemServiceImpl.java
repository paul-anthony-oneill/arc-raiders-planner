package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    public ItemServiceImpl(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    public List<Item> searchItems(String query) {
        return itemRepository.findByNameContainingIgnoreCase(query);
    }

    @Override
    public List<Item> getAllItems() {
        return itemRepository.findTop50ByOrderByNameAsc();
    }
}
