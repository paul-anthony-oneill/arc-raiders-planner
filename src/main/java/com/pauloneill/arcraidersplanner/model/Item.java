package com.pauloneill.arcraidersplanner.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    private String rarity;

    // Consumable, Nature, Advanced Material, Weapon etc
    @Column(name = "item_type")
    private String itemType;

    // Mechanical, Industrial etc
    @ManyToOne
    @JoinColumn(name = "loot_type_id")
    private LootType lootType;

    private String iconUrl;
    @Column(name = "item_value")
    private Integer value;
    private Double weight;
    private Integer stackSize;

    // Workbench used to craft this item (e.g., "Refiner", "Assembler", "Scrappy")
    // WHY: Enables tracking "upgrade Refiner to Level 3" as targetable objectives
    private String workbench;
}