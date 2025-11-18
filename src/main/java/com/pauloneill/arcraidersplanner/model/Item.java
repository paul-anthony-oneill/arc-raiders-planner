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
    @JoinColumn(name = "loot_area_id")
    private LootArea lootArea;

    private String iconUrl;
    private Integer value;
    private Double weight;
    private Integer stackSize;
}