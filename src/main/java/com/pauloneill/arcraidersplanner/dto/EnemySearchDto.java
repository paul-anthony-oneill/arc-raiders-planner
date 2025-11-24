package com.pauloneill.arcraidersplanner.dto;

import java.util.List;

/**
 * Response wrapper for enemy search operations.
 * WHY: Provides pagination support and total count for frontend UI
 */
public record EnemySearchDto(
        List<EnemyDto> enemies,
        int total
) {
}
