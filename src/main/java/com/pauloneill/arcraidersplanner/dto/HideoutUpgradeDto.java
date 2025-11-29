package com.pauloneill.arcraidersplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HideoutUpgradeDto(
    String id,
    Map<String, String> name,
    int maxLevel,
    List<UpgradeLevel> levels
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UpgradeLevel(
        int level,
        List<Requirement> requirementItemIds
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Requirement(
        String itemId,
        int quantity
    ) {}
}
