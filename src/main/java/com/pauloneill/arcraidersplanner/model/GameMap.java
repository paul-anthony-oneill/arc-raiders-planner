package com.pauloneill.arcraidersplanner.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "maps")
public class GameMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    private String imageUrl;

    @OneToMany(mappedBy = "gameMap", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Area> areas;

    @Column(name = "cal_scale_x")
    private Double calibrationScaleX = 1.0;

    @Column(name = "cal_scale_y")
    private Double calibrationScaleY = 1.0;

    @Column(name = "cal_offset_x")
    private Double calibrationOffsetX = 0.0;

    @Column(name = "cal_offset_y")
    private Double calibrationOffsetY = 0.0;
}