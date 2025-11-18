package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemRepository itemRepository;

    public ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /**
     * Endpoint: GET /api/items
     * Returns a list of all items currently synced in the database.
     */
    @GetMapping
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }
}