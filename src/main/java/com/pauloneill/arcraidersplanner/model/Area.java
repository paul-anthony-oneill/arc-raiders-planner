package com.pauloneill.arcraidersplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "areas")
public class Area implements RoutablePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "area_loot_type", joinColumns = @JoinColumn(name = "area_id"), inverseJoinColumns = @JoinColumn(name = "loot_type_id"))
    private Set<LootType> lootTypes = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    @JsonProperty
    private String coordinates;

    @ManyToOne
    @JoinColumn(name = "map_id", nullable = false)
    private GameMap gameMap;

    @Column(name = "map_x")
    private Integer mapX;

    @Column(name = "map_y")
    private Integer mapY;

    @Column(name = "loot_abundance")
    private Integer lootAbundance;

    @Override
    public String getId() {
        return String.valueOf(this.id);
    }

    @Override
    public double getX() {
        return this.mapX != null ? this.mapX : 0.0;
    }

    @Override
    public double getY() {
        return this.mapY != null ? this.mapY : 0.0;
    }

    @Override
    public String getName() {
        return this.name;
    }
}