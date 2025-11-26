package com.pauloneill.arcraidersplanner.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class AreaDto {
    private Long id;
    private String name;
    private Integer mapX;
    private Integer mapY;
    private Set<String> lootTypes;
    private String coordinates;
    private Integer lootAbundance; // 1 - Abundant, 2- Decent, 3 - Sparse
    private List<String> ongoingMatchItems; // Items from the ongoing list found in this area
}
