package com.barboleda.arbolado.domain;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Search result wrapper: distance-sorted items plus the normalized inputs that produced them.
 *
 * <p>{@code total} is the returned item count capped at {@code MAX_ITEMS} — never a
 * true match count. {@code truncated} is true only when more than {@code MAX_ITEMS}
 * matched. Empty results carry an empty list with zero total, never 404.
 *
 * @param items distance-sorted page, ascending
 * @param total item count after slicing, capped at {@code MAX_ITEMS}
 * @param truncated true when the probe detected more than {@code MAX_ITEMS} matches
 * @param normalizedLatitude grid-aligned request latitude
 * @param normalizedLongitude grid-aligned request longitude
 * @param radiusMeters rounded request radius in whole meters
 */
public record SearchResponse(
        @ArraySchema(arraySchema = @Schema(description = "Distance-sorted page, ascending"),
                schema = @Schema(implementation = ArbolResponse.class)) List<ArbolResponse> items,
        @Schema(description = "Item count after slicing, capped at MAX_ITEMS") int total,
        @Schema(description = "True when more than MAX_ITEMS matched") boolean truncated,
        @Schema(description = "Grid-aligned request latitude") double normalizedLatitude,
        @Schema(description = "Grid-aligned request longitude") double normalizedLongitude,
        @Schema(description = "Rounded request radius in whole meters") int radiusMeters)
{
}
