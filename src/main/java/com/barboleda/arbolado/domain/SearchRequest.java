package com.barboleda.arbolado.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Geospatial search input: a center point plus a radius in meters.
 *
 * <p>Boxed {@code Double} with {@code @NotNull} so missing fields are rejected
 * instead of defaulting to zero. Fractional radii pass validation and are rounded
 * to whole meters by {@code ArbolService} afterwards.
 *
 * @param latitude search center latitude in degrees, ±90
 * @param longitude search center longitude in degrees, ±180
 * @param radius search radius in meters, 1 to 1000
 */
public record SearchRequest(
        @Schema(description = "Search center latitude in degrees", example = "-34.6037")
        @NotNull
        @DecimalMin(SearchLimits.MIN_LATITUDE_STR)
        @DecimalMax(SearchLimits.MAX_LATITUDE_STR)
        Double latitude,
        @Schema(description = "Search center longitude in degrees", example = "-58.3816")
        @NotNull
        @DecimalMin(SearchLimits.MIN_LONGITUDE_STR)
        @DecimalMax(SearchLimits.MAX_LONGITUDE_STR)
        Double longitude,
        @Schema(description = "Search radius in meters", example = "80")
        @NotNull
        @DecimalMin(SearchLimits.MIN_RADIUS_STR)
        @DecimalMax(SearchLimits.MAX_RADIUS_STR)
        Double radius)
{
}
