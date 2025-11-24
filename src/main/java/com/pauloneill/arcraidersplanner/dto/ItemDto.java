package com.pauloneill.arcraidersplanner.dto;

import lombok.Data;

@Data
public class ItemDto {
    private Long id;
    private String name;
    private String description;
    private String rarity;
    private String itemType;
    private String lootType; // Just the name of the loot type
    private String iconUrl;
    private Integer value;
    private Double weight;
    private Integer stackSize;
}
