package it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.barboleda.arbolado.domain.Arbol;
import com.barboleda.arbolado.service.MongoArbolSearchAdapter;
import com.mongodb.client.MongoClients;

/**
 * Real-Mongo geospatial proofs against Testcontainers {@code mongo:7}.
 *
 * <p>CI-gated only ({@code -Pintegration}): needs docker, never runs in the default
 * build. No Spring context — plain driver wiring keeps the suite hermetic.
 *
 * <p>Writes below (seed/drop/ensure) target only this ephemeral container; the app
 * itself is read-only and never creates indexes (the prod {@code 2dsphere} index
 * is owned out-of-band by the ETL/infra).
 */
@Testcontainers
@Tag("integration")
class GeoSpatialIT
{

    private static final double METERS_PER_DEGREE_LAT = 111320.0;

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    private MongoTemplate template;

    private MongoArbolSearchAdapter adapter;

    /**
     * Opens a fresh database with the {@code 2dsphere} index before each test.
     */
    @BeforeEach
    void setUp()
    {
        template = new MongoTemplate(MongoClients.create(MONGO.getConnectionString()), "arbolado_it");
        template.dropCollection(Arbol.class);
        template.indexOps(Arbol.class)
                .ensureIndex(new GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE));
        adapter = new MongoArbolSearchAdapter(template);
    }

    @Test
    @DisplayName("one-meter radius behaves in meters, not kilometers")
    void metersAtOneMeterScale()
    {
        // Given a tree about two meters north of the center
        insertTree("close", 0.0, metersToDegrees(2.0));

        // When searching a 1m radius versus a 3m radius
        // Then the meter scale holds both ways
        assertThat(adapter.searchNear(new Point(0.0, 0.0), new Distance(0.001, Metrics.KILOMETERS))).isEmpty();
        assertThat(adapter.searchNear(new Point(0.0, 0.0), new Distance(0.003, Metrics.KILOMETERS)))
                .extracting(Arbol::getId)
                .containsExactly("close");
    }

    @Test
    @DisplayName("thousand-meter radius behaves in meters")
    void metersAtFullScale()
    {
        // Given a tree about 991 meters north of the center
        insertTree("edge", 0.0, metersToDegrees(991.0));

        // When searching 990m versus 1000m
        // Then the ceiling behaves in meters
        assertThat(adapter.searchNear(new Point(0.0, 0.0), new Distance(0.99, Metrics.KILOMETERS))).isEmpty();
        assertThat(adapter.searchNear(new Point(0.0, 0.0), new Distance(1.0, Metrics.KILOMETERS)))
                .extracting(Arbol::getId)
                .containsExactly("edge");
    }

    @Test
    @DisplayName("results arrive in ascending distance order")
    void distanceSorted()
    {
        // Given trees at 100m, 10m and 50m north of the center
        insertTree("far", 0.0, metersToDegrees(100.0));
        insertTree("near", 0.0, metersToDegrees(10.0));
        insertTree("mid", 0.0, metersToDegrees(50.0));

        // When searching
        // Then Mongo returns them nearest-first regardless of insert order
        assertThat(adapter.searchNear(new Point(0.0, 0.0), new Distance(1.0, Metrics.KILOMETERS)))
                .extracting(Arbol::getId)
                .containsExactly("near", "mid", "far");
    }

    @Test
    @DisplayName("geoNear without the 2dsphere index fails, index is out-of-band")
    void indexMissingBreaksGeoNear()
    {
        // Given a seed without the out-of-band 2dsphere index
        template.indexOps(Arbol.class).dropIndex("location_2dsphere");
        insertTree("seeded", -58.3816, -34.6037);

        // When searching
        // Then Mongo refuses the geo query — the index must exist out-of-band
        assertThatThrownBy(() -> adapter.searchNear(new Point(-58.3816, -34.6037),
                new Distance(1.0, Metrics.KILOMETERS))).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("antimeridian wrap matches across ±180")
    void antimeridianWrap()
    {
        // Given a tree just east of the antimeridian (0.0002° ≈ 22m away across the wrap)
        insertTree("wrapped", 179.9999, 0.0);

        // When searching from just west of the antimeridian
        // Then the 30m radius wraps and matches, while the 10m radius does not
        assertThat(adapter.searchNear(new Point(-179.9999, 0.0), new Distance(0.03, Metrics.KILOMETERS)))
                .extracting(Arbol::getId)
                .containsExactly("wrapped");
        assertThat(adapter.searchNear(new Point(-179.9999, 0.0), new Distance(0.01, Metrics.KILOMETERS))).isEmpty();
    }

    @Test
    @DisplayName("overflow probe survives the round trip capped at 101")
    void probeIntact()
    {
        // Given 102 trees inside the radius
        IntStream.range(0, 102).forEach(i -> insertTree("tree-" + i, 0.0, metersToDegrees(10.0 + i * 0.1)));

        // When searching
        // Then the MAX_ITEMS + 1 probe arrives intact for the service to slice
        List<Arbol> found = adapter.searchNear(new Point(0.0, 0.0), new Distance(1.0, Metrics.KILOMETERS));
        assertThat(found).hasSize(101);
    }

    private void insertTree(String id, double longitude, double latitude)
    {
        template.insert(new Arbol(id, new GeoJsonPoint(longitude, latitude), 1, "Eucalyptus", 10, 40, 1, longitude,
                latitude, "csv", false));
    }

    private double metersToDegrees(double meters)
    {
        return meters / METERS_PER_DEGREE_LAT;
    }
}
