package com.barboleda.arbolado.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.NearQuery;

import com.barboleda.arbolado.domain.Arbol;

/**
 * Adapter contract over a mocked {@link MongoTemplate} (PLAN.md #4, #8).
 */
class MongoArbolSearchAdapterTest
{

    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);

    private final MongoArbolSearchAdapter adapter = new MongoArbolSearchAdapter(mongoTemplate);

    private static final Point CENTER = new Point(-58.3816, -34.6037);

    private static final Distance HALF_KM = new Distance(0.5, Metrics.KILOMETERS);

    @Test
    @DisplayName("GeoResults unwrap to entities in the given order")
    void unwrapsInOrder()
    {
        // Given distance-sorted geo results from Mongo
        Arbol near = tree("near");
        Arbol far = tree("far");
        stubResults(near, far);

        // When searching
        List<Arbol> found = adapter.searchNear(CENTER, HALF_KM);

        // Then entities come back unwrapped and never reordered or leaked as GeoResults
        assertThat(found).containsExactly(near, far);
    }

    @Test
    @DisplayName("query is spherical with a 101 probe and meter wire distance")
    void sphericalProbeWithMeterWireDistance()
    {
        // Given stubbed results
        stubResults(tree("x"));

        // When searching a 500m radius
        adapter.searchNear(CENTER, new Distance(0.5, Metrics.KILOMETERS));

        // Then the captured query is spherical, probes MAX_ITEMS + 1, and the wire
        // radians × earth-radius-km × 1000 land back on meters (the driver takes radians)
        ArgumentCaptor<NearQuery> captor = ArgumentCaptor.forClass(NearQuery.class);
        verify(mongoTemplate).geoNear(captor.capture(), eq(Arbol.class));
        NearQuery query = captor.getValue();
        assertThat(query.isSpherical()).isTrue();
        Document wire = query.toDocument();
        assertThat(wire.getLong("num")).isEqualTo(101L);
        double meters = wire.getDouble("maxDistance") * wire.getDouble("distanceMultiplier") * 1000.0;
        assertThat(meters).isCloseTo(500.0, within(1e-6));
    }

    @Test
    @DisplayName("empty results map to an empty list")
    void emptyMapsToEmpty()
    {
        // Given no matches in Mongo
        when(mongoTemplate.geoNear(any(NearQuery.class), eq(Arbol.class)))
                .thenReturn(new GeoResults<>(List.of()));

        // When searching
        // Then the result is empty (never null, never 404 — the service wraps it)
        assertThat(adapter.searchNear(CENTER, HALF_KM)).isEmpty();
    }

    @Test
    @DisplayName("Mongo failures bubble up for the 503 mapping")
    void failureBubbles()
    {
        // Given a Mongo outage
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("down");
        when(mongoTemplate.geoNear(any(NearQuery.class), eq(Arbol.class))).thenThrow(failure);

        // When searching
        // Then the failure propagates untouched (the handler maps it to 503)
        assertThatThrownBy(() -> adapter.searchNear(CENTER, HALF_KM)).isSameAs(failure);
    }

    private void stubResults(Arbol... trees)
    {
        List<GeoResult<Arbol>> contents = Arrays.stream(trees)
                .map(tree -> new GeoResult<>(tree, new Distance(0.1, Metrics.KILOMETERS)))
                .toList();
        when(mongoTemplate.geoNear(any(NearQuery.class), eq(Arbol.class)))
                .thenReturn(new GeoResults<>(contents));
    }

    private Arbol tree(String id)
    {
        return new Arbol(id, new GeoJsonPoint(-58.3816, -34.6037), 1, "Eucalyptus", 10, 40, 1, -58.3816, -34.6037,
                "csv", false);
    }
}
