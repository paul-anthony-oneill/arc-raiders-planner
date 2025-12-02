package com.pauloneill.arcraidersplanner.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a cluster of nearby container spawn markers.
 * WHY: Groups container spawns into logical zones for cleaner routing.
 */
@Data
@Entity
@Table(name = "marker_groups")
public class MarkerGroup implements RoutablePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // "Dam - Red Locker Zone 1"

    @ManyToOne
    @JoinColumn(name = "map_id", nullable = false)
    private GameMap gameMap;

    @ManyToOne
    @JoinColumn(name = "container_type_id", nullable = false)
    private ContainerType containerType;

    // Center point (average of all markers in group)
    @Column(nullable = false)
    private Double centerLat;

    @Column(nullable = false)
    private Double centerLng;

    // Metadata
    @Column(nullable = false)
    private Integer markerCount; // How many individual markers in this group

    private Double radius; // Effective radius of the zone

    // Reverse relationship to markers
    @OneToMany(mappedBy = "markerGroup", fetch = FetchType.LAZY)
    private List<MapMarker> markers = new ArrayList<>();

    public Long getDatabaseId() {
        return this.id;
    }

    // RoutablePoint implementation
    @Override
    public String getId() {
        return "group_" + this.id;
    }

    @Override
    public double getX() {
        return this.centerLng;
    }

    @Override
    public double getY() {
        return this.centerLat;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
