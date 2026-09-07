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
 * <p>Isolates geo-query complexity (SRP). Derived {@code findByLocationNear} cannot
 * express a limit, hence {@code MongoTemplate} + {@code NearQuery}. The
 * {@code MAX_ITEMS + 1} probe detects overflow exactly; slicing stays with the service.
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
        Point point = new Point(center.longitude(), center.latitude());
        Distance maxDistance = new Distance(radius.toKilometers(), Metrics.KILOMETERS);

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
