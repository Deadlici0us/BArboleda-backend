package com.barboleda.arbolado.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Geospatial search input: a center point.
 *
 * @param latitude search center latitude in degrees, ±90
 * @param longitude search center longitude in degrees, ±180
 */
public record SearchRequest(
        @Schema(description = "Search center latitude in degrees", example = "-34.6037",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin(SearchLimits.MIN_LATITUDE_STR)
        @DecimalMax(SearchLimits.MAX_LATITUDE_STR)
        Double latitude,
        @Schema(description = "Search center longitude in degrees", example = "-58.3816",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin(SearchLimits.MIN_LONGITUDE_STR)
        @DecimalMax(SearchLimits.MAX_LONGITUDE_STR)
        Double longitude)
{
}
