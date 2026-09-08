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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.barboleda.arbolado.domain.Arbol;
import com.barboleda.arbolado.service.GeoCenter;
import com.barboleda.arbolado.service.MongoArbolSearchAdapter;
import com.barboleda.arbolado.service.RadiusMeters;
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
    @DisplayName("fixed 1080m padded query (fixed bucket)")
    void metersAtOneMeterScale()
    {
        // Given a tree 2m north; adapter always queries 1080m
        insertTree("close", 0.0, metersToDegrees(2.0));

        // When searching
        // Then adapter returns the same fixed-bucket result regardless
        assertThat(adapter.searchNear(new GeoCenter(0.0, 0.0), new RadiusMeters(1)))
                .extracting(Arbol::getId).containsExactly("close");
        assertThat(adapter.searchNear(new GeoCenter(0.0, 0.0), new RadiusMeters(3)))
                .extracting(Arbol::getId).containsExactly("close");
        assertThat(adapter.searchNear(new GeoCenter(0.0, 0.0), new RadiusMeters(1000)))
                .extracting(Arbol::getId).containsExactly("close");
    }

    @Test
    @DisplayName("fixed 1080m padded query finds trees within true 1000m circle")
    void metersAtFullScale()
    {
        // Given a tree 991m north; adapter always queries 1080m (padded)
        insertTree("edge", 0.0, metersToDegrees(991.0));

        // When searching
        // Then fixed bucket finds the tree (within 1080m, well inside 1000m true circle + padding)
        assertThat(adapter.searchNear(new GeoCenter(0.0, 0.0), new RadiusMeters(990)))
                .extracting(Arbol::getId).containsExactly("edge");
        assertThat(adapter.searchNear(new GeoCenter(0.0, 0.0), new RadiusMeters(1000)))
                .extracting(Arbol::getId).containsExactly("edge");
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
        assertThat(adapter.searchNear(new GeoCenter(0.0, 0.0), new RadiusMeters(1000)))
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
        assertThatThrownBy(() -> adapter.searchNear(new GeoCenter(-58.3816, -34.6037),
                new RadiusMeters(1000))).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("antimeridian wrap matches across ±180 with fixed 1080m query")
    void antimeridianWrap()
    {
        // Given a tree just east of the antimeridian (~22m across wrap)
        insertTree("wrapped", 179.9999, 0.0);

        // When searching from just west (fixed 1080m bucket covers wrap easily)
        assertThat(adapter.searchNear(new GeoCenter(-179.9999, 0.0), new RadiusMeters(30)))
                .extracting(Arbol::getId)
                .containsExactly("wrapped");
        assertThat(adapter.searchNear(new GeoCenter(-179.9999, 0.0), new RadiusMeters(10)))
                .extracting(Arbol::getId)
                .containsExactly("wrapped");
    }

    @Test
    @DisplayName("overflow probe survives capped at 1001 (MAX_ITEMS + 1)")
    void probeIntact()
    {
        // Given 1005 trees inside the 1080m padded radius (exceeds MAX_ITEMS + 1 = 1001)
        IntStream.range(0, 1005).forEach(i -> insertTree("tree-" + i, 0.0, metersToDegrees(10.0 + i * 0.1)));

        // When searching
        // Then Mongo returns the capped 1001-item probe intact (MAX_ITEMS + 1)
        List<Arbol> found = adapter.searchNear(new GeoCenter(0.0, 0.0), new RadiusMeters(1000));
        assertThat(found).hasSize(1001); // capped at 1001 by adapter limit
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
