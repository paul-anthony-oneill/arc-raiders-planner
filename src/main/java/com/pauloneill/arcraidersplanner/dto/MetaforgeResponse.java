package com.pauloneill.arcraidersplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MetaforgeResponse<T>(
        List<T> data,
        @JsonProperty("pagination") Pagination pagination
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagination(
            int page,
            @JsonProperty("per_page") int perPage,
            @JsonProperty("totalPages") int totalPages
    ) {
    }
}
