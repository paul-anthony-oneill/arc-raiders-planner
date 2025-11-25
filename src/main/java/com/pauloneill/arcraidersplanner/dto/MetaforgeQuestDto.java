package com.pauloneill.arcraidersplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MetaforgeQuestDto(
        String id,
        String name
) {
}
