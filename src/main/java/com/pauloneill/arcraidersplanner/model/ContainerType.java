package com.pauloneill.arcraidersplanner.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Represents a type of loot container that players can target.
 * WHY: Simple catalog for player-selectable container types - no loot estimation.
 */
@Data
@Entity
@Table(name = "container_types")
public class ContainerType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // "Red Locker"

    @Column(nullable = false, unique = true)
    private String subcategory; // "red-locker" (matches MapMarker.subcategory)

    @Column(columnDefinition = "TEXT")
    private String description;

    private String iconUrl;
}
