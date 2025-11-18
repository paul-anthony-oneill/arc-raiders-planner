package com.pauloneill.arcraidersplanner.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "areas")
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "loot_area_id", nullable = false)
    private LootArea lootArea;

    @ManyToOne
    @JoinColumn(name = "map_id", nullable = false)
    private GameMap gameMap;

    @Column(name = "map_x")
    private Integer mapX;

    @Column(name = "map_y")
    private Integer mapY;
}