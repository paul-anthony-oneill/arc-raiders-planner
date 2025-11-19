package com.pauloneill.arcraidersplanner.dto;

import lombok.Data;

import java.util.Set;

@Data
public class AreaDto {
    private Long id;
    private String name;
    private Integer mapX;
    private Integer mapY;
    private Set<String> lootTypes; // Just the names
}
