package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.model.Item;

import java.util.List;

public interface ItemService {
    List<Item> searchItems(String query);

    List<Item> getAllItems();
}
