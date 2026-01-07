package com.pauloneill.arcraidersplanner.dto;

/**
 * DTO for ContainerType catalog (simplified).
 */
public record ContainerTypeDto(
    Long id,
    String name,
    String subcategory,
    String description,
    String iconUrl
) {}
