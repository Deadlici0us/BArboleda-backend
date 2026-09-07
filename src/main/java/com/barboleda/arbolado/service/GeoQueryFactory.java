package com.barboleda.arbolado.service;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Component;

import com.barboleda.arbolado.domain.SearchLimits;

/**
 * Builds lon-first geospatial queries with meter-to-kilometer conversion.
 *
 * <p>The {@code 2dsphere} query needs kilometers while the API speaks meters, so the
 * divisor must stay floating-point: {@code radius / 1000} on an {@code int} would
 * truncate to zero for every radius under 1000m.
 */
@Component
public class GeoQueryFactory
{

    /**
     * Creates the query for one normalized search.
     *
     * @param longitude normalized longitude, must be non-null
     * @param latitude normalized latitude, must be non-null
     * @param radiusMeters already-rounded radius, 1 to {@code MAX_RADIUS_METERS}
     * @return the center point plus KM distance
     * @throws IllegalArgumentException on null coordinates or out-of-range radius
     */
    public GeoQuery create(Double longitude, Double latitude, int radiusMeters)
    {
        if (longitude == null || latitude == null)
        {
            throw new IllegalArgumentException("Geo query coordinates must not be null");
        }
        if (radiusMeters < SearchLimits.MIN_RADIUS_METERS || radiusMeters > SearchLimits.MAX_RADIUS_METERS)
        {
            throw new IllegalArgumentException("Geo query radius must be 1..1000m, got " + radiusMeters);
        }
        return new GeoQuery(new Point(longitude, latitude),
                new Distance(radiusMeters / 1000.0, Metrics.KILOMETERS));
    }
}
