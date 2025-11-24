package com.pauloneill.arcraidersplanner.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "map_markers")
public class MapMarker {

    @Id
    @Column(unique = true)
    private String id; // UUID from the API

    @Column(nullable = false)
    private Double lat; // Raw external Y

    @Column(nullable = false)
    private Double lng; // Raw external X

    private String category;    // e.g. "arc", "nature"
    private String subcategory; // e.g. "sentinel", "great-mullein"
    private String name;        // User-defined label (nullable)

    // RELATIONSHIP: Link to the GameMap
    @ManyToOne
    @JoinColumn(name = "map_id", nullable = false)
    private GameMap gameMap;
}