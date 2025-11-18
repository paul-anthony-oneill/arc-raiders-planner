package com.pauloneill.arcraidersplanner.dto;

import java.util.List;

public record MetaforgeResponse<T>(
        List<T> data,
        int page,
        int total
) {
}