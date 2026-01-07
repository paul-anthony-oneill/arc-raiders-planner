package com.pauloneill.arcraidersplanner.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

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

    // Links to Metaforge API item.id
    // WHY: Required for mapping recipes and external data references
    @Column(name = "metaforge_id", unique = true)
    private String metaforgeId;

    // Mechanical, Industrial etc
    @ManyToOne
    @JoinColumn(name = "loot_type_id")
    private LootType lootType;

    private String iconUrl;
    @Column(name = "item_value")
    private Integer value;
    private Double weight;
    private Integer stackSize;

    @ElementCollection
    @CollectionTable(name = "item_dropped_by", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "enemy_id")
    private Set<String> droppedBy = new HashSet<>();
}