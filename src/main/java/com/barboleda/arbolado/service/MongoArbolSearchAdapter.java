package com.barboleda.arbolado.service;

import java.util.List;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.barboleda.arbolado.domain.Arbol;
import com.barboleda.arbolado.domain.SearchLimits;

/**
 * Mongo adapter behind {@link ArbolSearchPort}: the only path that touches geo queries.
 *
 * <p>The adapter always queries the fixed 1080m padded radius (1000m + snap-shift cover).
 * Limit is {@code MAX_ITEMS + 1} (1001) for overflow detection.
 * All Spring Data geo types stay inside this adapter — the service layer never sees them.
 */
@Component
public class MongoArbolSearchAdapter implements ArbolSearchPort
{

    private final MongoTemplate mongoTemplate;

    /**
     * Builds the adapter over the shared template.
     *
     * @param mongoTemplate the configured template, never null
     */
    public MongoArbolSearchAdapter(MongoTemplate mongoTemplate)
    {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Arbol> searchNear(GeoCenter center, RadiusMeters radius)
    {
        // Fixed 1000m bucket with ~78m half-diagonal padding covers true 1000m circle
        // despite 3-decimal snap shift.
        Point point = new Point(center.longitude(), center.latitude());
        Distance maxDistance = new Distance(
                SearchLimits.MONGO_QUERY_RADIUS_METERS / 1000.0, Metrics.KILOMETERS);

        Query projection = new Query();
        projection.fields().include("id", "location", "nro_registro", "nombre_cientifico",
                "altura_arbol", "diametro_altura_pecho", "comuna", "long", "lat");
        NearQuery query = NearQuery.near(point)
                .spherical(true)
                .maxDistance(maxDistance)
                .limit(SearchLimits.MAX_ITEMS + 1)
                .query(projection);
        return mongoTemplate.geoNear(query, Arbol.class).getContent().stream()
                .map(GeoResult::getContent).toList();
    }
}
