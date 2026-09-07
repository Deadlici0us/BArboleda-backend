package com.barboleda.arbolado.domain;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API and cache payload for one tree: snake_case wire names matching the ETL keys.
 *
 * <p>Carries plain {@code long}/{@code lat} doubles — never a {@code GeoJsonPoint} —
 * so the Redis JSON stays plain. Null fields coalesce to default values: numeric fields
 * become {@code 0} or {@code 0.0}, strings become {@code ""}.
 *
 * @param nroRegistro registry number
 * @param nombreCientifico scientific name
 * @param alturaArbol tree height
 * @param diametroAlturaPecho diameter at breast height
 * @param comuna district number
 * @param longitude ETL longitude, served as {@code long}
 * @param latitude ETL latitude, served as {@code lat}
 */
@Schema(description = "Single street tree response")
public record ArbolResponse(
        @Schema(description = "Registry number", example = "123",
                requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = false, defaultValue = "0")
        @JsonProperty("nro_registro") int nroRegistro,
        @Schema(description = "Scientific name", example = "Jacaranda mimosifolia",
                requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = false, defaultValue = "")
        @JsonProperty("nombre_cientifico") String nombreCientifico,
        @Schema(description = "Tree height", example = "8",
                requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = false, defaultValue = "0")
        @JsonProperty("altura_arbol") int alturaArbol,
        @Schema(description = "Diameter at breast height", example = "30",
                requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = false, defaultValue = "0")
        @JsonProperty("diametro_altura_pecho") int diametroAlturaPecho,
        @Schema(description = "District number", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = false, defaultValue = "0")
        @JsonProperty("comuna") int comuna,
        @Schema(description = "Longitude", example = "-58.3816",
                requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = false, defaultValue = "0.0")
        @JsonProperty("long") double longitude,
        @Schema(description = "Latitude", example = "-34.6037",
                requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = false, defaultValue = "0.0")
        @JsonProperty("lat") double latitude)
{

    public ArbolResponse
    {
        nombreCientifico = Objects.requireNonNullElse(nombreCientifico, "");
    }
}
