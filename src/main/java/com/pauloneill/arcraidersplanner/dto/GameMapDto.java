package com.pauloneill.arcraidersplanner.dto;

import lombok.Data;

import java.util.Set;

@Data
public class GameMapDto {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Set<AreaDto> areas;
    private Double calibrationScaleX;
    private Double calibrationScaleY;
    private Double calibrationOffsetX;
    private Double calibrationOffsetY;
}
