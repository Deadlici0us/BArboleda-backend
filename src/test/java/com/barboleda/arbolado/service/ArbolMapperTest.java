package com.barboleda.arbolado.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import com.barboleda.arbolado.domain.Arbol;
import com.barboleda.arbolado.domain.ArbolResponse;

/**
 * Entity-to-DTO mapping contract (PLAN.md #3, #8).
 */
class ArbolMapperTest
{

    private final ArbolMapper mapper = new ArbolMapper();

    @Test
    @DisplayName("full ETL entity maps 1:1 onto the response")
    void fullEntityMapsOneToOne()
    {
        // Given an entity with every contracted field populated
        Arbol entity = new Arbol("abc123", new GeoJsonPoint(-58.3816, -34.6037), 123,
                "Jacaranda mimosifolia", 8, 30, 1, -58.3816, -34.6037, "csv", false);

        // When mapping
        ArbolResponse response = mapper.toResponse(entity);

        // Then every field transfers exactly (null source/es_merged excluded; primitives coalesced)
        assertThat(response.nroRegistro()).isEqualTo(123);
        assertThat(response.nombreCientifico()).isEqualTo("Jacaranda mimosifolia");
        assertThat(response.alturaArbol()).isEqualTo(8);
        assertThat(response.diametroAlturaPecho()).isEqualTo(30);
        assertThat(response.comuna()).isEqualTo(1);
        assertThat(response.longitude()).isEqualTo(-58.3816);
        assertThat(response.latitude()).isEqualTo(-34.6037);
    }

    @Test
    @DisplayName("GeoJSON x/y maps lon-first onto long/lat")
    void lonFirstOrdering()
    {
        // Given a point where x != y so a flip is visible
        Arbol entity = new Arbol("x", new GeoJsonPoint(10.0, 20.0), null, null, null, null, null, null, null, null,
                null);

        // When mapping
        ArbolResponse response = mapper.toResponse(entity);

        // Then x lands on long and y on lat (never flipped), null fields coalesce to defaults
        assertThat(response.longitude()).isEqualTo(10.0);
        assertThat(response.latitude()).isEqualTo(20.0);
        assertThat(response.nroRegistro()).isEqualTo(0);
        assertThat(response.nombreCientifico()).isEqualTo("");
    }

    @Test
    @DisplayName("null location and null optionals coalesce to defaults")
    void nullsMapToDefaults()
    {
        // Given an entity with null location and null optionals
        Arbol entity = new Arbol("x", null, null, null, null, null, null, null, null, null, null);

        // When mapping
        ArbolResponse response = mapper.toResponse(entity);

        // Then coordinates and optionals coalesce to defaults (0 / 0.0 / "")
        assertThat(response.longitude()).isEqualTo(0.0);
        assertThat(response.latitude()).isEqualTo(0.0);
        assertThat(response.nroRegistro()).isEqualTo(0);
        assertThat(response.alturaArbol()).isEqualTo(0);
        assertThat(response.diametroAlturaPecho()).isEqualTo(0);
        assertThat(response.comuna()).isEqualTo(0);
        assertThat(response.nombreCientifico()).isEqualTo("");
    }
}
