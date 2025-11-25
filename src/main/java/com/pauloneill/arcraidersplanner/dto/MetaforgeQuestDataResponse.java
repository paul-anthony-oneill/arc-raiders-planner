package com.pauloneill.arcraidersplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

public record MetaforgeQuestDataResponse<T>(
    List<T> data,
    Pagination pagination
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagination(
            int page,
            int limit,
            int total,
            int totalPages,
            boolean hasNextPage,
            boolean hasPrevPage
    ) {}
}
