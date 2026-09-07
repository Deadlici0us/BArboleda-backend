package com.barboleda.arbolado.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.barboleda.arbolado.domain.Arbol;
import com.barboleda.arbolado.domain.ArbolResponse;
import com.barboleda.arbolado.domain.SearchLimits;

/**
 * Cached-search delegation contract with a mocked port and mapper (PLAN.md #5, #8).
 */
class CachedArbolSearchTest
{

    private final ArbolSearchPort port = mock(ArbolSearchPort.class);

    private final ArbolMapper mapper = mock(ArbolMapper.class);

    private final CachedArbolSearch cached = new CachedArbolSearch(port, mapper);

    @Test
    @DisplayName("port receives a lon-first point plus kilometer distance")
    void delegatesLonFirstKmQuery()
    {
        // Given one stored tree mapping to one DTO
        Arbol entity = new Arbol("x", null, 1, "Eucalyptus", 10, 40, 1, -58.3816, -34.6037, "csv", false);
        ArbolResponse dto = new ArbolResponse(1, "Eucalyptus", 10, 40, 1, -58.3816, -34.6037);
        when(port.searchNear(any(GeoCenter.class), any(RadiusMeters.class))).thenReturn(List.of(entity));
        when(mapper.toResponse(entity)).thenReturn(dto);

        // When searching the cached path
        List<ArbolResponse> found = cached.findNearbyCached(-34.6037, -58.3816, new RadiusMeters(500));

        // Then the port saw Point(lon, lat) with 0.5 km and the DTO came back
        org.mockito.Mockito.verify(port).searchNear(any(GeoCenter.class), any(RadiusMeters.class));
        assertThat(found).containsExactly(dto);
    }

    @Test
    @DisplayName("up to MAX_ITEMS + 1 DTOs map with no slicing")
    void mapsProbeWithoutSlicing()
    {
        // Given a full probe page from the port
        Arbol[] entities = new Arbol[SearchLimits.MAX_ITEMS + 1];
        Arrays.setAll(entities, i -> new Arbol("id-" + i, null, i, "E", 1, 1, 1, 0.0, 0.0, "csv", false));
        when(port.searchNear(any(GeoCenter.class), any(RadiusMeters.class))).thenReturn(Arrays.asList(entities));
        when(mapper.toResponse(any(Arbol.class))).thenAnswer(call -> dtoFor((Arbol) call.getArgument(0)));

        // When searching
        List<ArbolResponse> found = cached.findNearbyCached(0.0, 0.0, new RadiusMeters(1000));

        // Then all 101 DTOs return — slicing is the service's job
        assertThat(found).hasSize(SearchLimits.MAX_ITEMS + 1);
    }

    @Test
    @DisplayName("empty port result stays empty")
    void emptyStaysEmpty()
    {
        // Given no matches
        when(port.searchNear(any(GeoCenter.class), any(RadiusMeters.class))).thenReturn(List.of());

        // When searching
        // Then the result is empty
        assertThat(cached.findNearbyCached(0.0, 0.0, new RadiusMeters(500))).isEmpty();
    }

    private ArbolResponse dtoFor(Arbol entity)
    {
        double longitude = (entity.getLongitude() != null) ? entity.getLongitude() : 0.0;
        double latitude = (entity.getLatitude() != null) ? entity.getLatitude() : 0.0;
        return new ArbolResponse(
                (entity.getNroRegistro() != null) ? entity.getNroRegistro() : 0,
                (entity.getNombreCientifico() != null) ? entity.getNombreCientifico() : "",
                (entity.getAlturaArbol() != null) ? entity.getAlturaArbol() : 0,
                (entity.getDiametroAlturaPecho() != null) ? entity.getDiametroAlturaPecho() : 0,
                (entity.getComuna() != null) ? entity.getComuna() : 0,
                longitude,
                latitude);
    }
}
