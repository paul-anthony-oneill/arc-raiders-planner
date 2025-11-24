package com.pauloneill.arcraidersplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MetaforgeMapDataResponse(
        List<MetaforgeMarkerDto> allData // <--- Matches the JSON key "allData"
) {
}