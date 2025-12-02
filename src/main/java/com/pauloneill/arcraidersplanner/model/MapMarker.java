package com.pauloneill.arcraidersplanner.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "map_markers")
public class MapMarker implements RoutablePoint {

    @Id
    @Column(unique = true)
    private String id; // UUID from the API

    @Column(nullable = false)
    private Double lat; // Leaflet-ready Y coordinate (calibrated, ready for [lat,lng] format)

    @Column(nullable = false)
    private Double lng; // Leaflet-ready X coordinate (calibrated, ready for [lat,lng] format)

    private String category;    // e.g. "arc", "nature"
    private String subcategory; // e.g. "sentinel", "great-mullein"
    private String name;        // User-defined label (nullable)

    // RELATIONSHIP: Link to the GameMap
    @ManyToOne
    @JoinColumn(name = "map_id", nullable = false)
    private GameMap gameMap;

    // NEW: Grouping information
    @ManyToOne
    @JoinColumn(name = "group_id")
    private MarkerGroup markerGroup; // NULL if standalone

    private Boolean isGrouped = false;

    private String standaloneReason; // "isolated", "unique_poi", "extraction_point"

    @Override
    public double getX() {
        return this.lng;
    }

    @Override
    public double getY() {
        return this.lat;
    }

    @Override
    public String getName() {
        return this.name;
    }
}