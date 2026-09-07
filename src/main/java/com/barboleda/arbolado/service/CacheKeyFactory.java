package com.barboleda.arbolado.service;

import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Builds fixed-precision cache keys from already-normalized coordinates.
 *
 * <p>A Spring bean so the {@code @Cacheable} key SpEL resolves
 * {@code @cacheKeyFactory} by name (PLAN.md #5). Key stability never depends on
 * callers normalizing first: {@code %.4f} collapses trailing decimals and signed
 * zero canonicalizes to {@code 0.0} ({@code %.4f} alone would render
 * {@code "-0.0000"}, a different key). Always receives the already-rounded
 * {@code int} radius from the service.
 */
@Component
public class CacheKeyFactory
{

    /**
     * Creates the cache key for one normalized search.
     *
     * @param latitude normalized latitude, must be finite and non-null
     * @param longitude normalized longitude, must be finite and non-null
     * @param radiusMeters already-rounded radius in whole meters
     * @return the key in {@code lat:lon:radius} form, e.g. {@code -34.6037:-58.3816:500}
     * @throws IllegalArgumentException if a coordinate is null, NaN or infinite
      */
    public String create(Double latitude, Double longitude, int radiusMeters)
    {
        requireCoordinate(latitude, "latitude");
        requireCoordinate(longitude, "longitude");
        double lat = latitude == 0.0 ? 0.0 : latitude;
        double lon = longitude == 0.0 ? 0.0 : longitude;
        return String.format(Locale.ROOT, "%.4f:%.4f:%.0f", lat, lon, (double) radiusMeters);
    }

    private void requireCoordinate(Double value, String name)
    {
        if (value == null || !Double.isFinite(value))
        {
            throw new IllegalArgumentException("Cache key coordinate " + name + " must be finite and non-null");
        }
    }
}
