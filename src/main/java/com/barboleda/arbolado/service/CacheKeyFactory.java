package com.barboleda.arbolado.service;

import org.springframework.stereotype.Component;

/**
 * Builds fixed-precision cache keys from already-normalized coordinates.
 *
 * <p>A Spring bean so the {@code @Cacheable} key SpEL resolves
 * {@code @cacheKeyFactory} by name. Key stability never depends on callers
 * normalizing first. Signed zero canonicalizes to {@code 0.0} so that
 * {@code -0.0000} and {@code 0.0000} share one cache entry. Always receives
 * the already-rounded {@code int} radius from the service.
 */
@Component
public class CacheKeyFactory implements CacheKeyGenerator
{

    private static final int SCALE = 4;
    private static final double SCALE_FACTOR = 10000.0;

    /**
     * Creates the cache key for one normalized search using scaled long arithmetic
     * (no {@code String.format} allocation on the hot path).
     *
     * @param latitude normalized latitude, must be finite and non-null
     * @param longitude normalized longitude, must be finite and non-null
     * @param radiusMeters already-rounded radius in whole meters
     * @return the key in {@code lat:lon:radius} form, e.g. {@code -34.6037:-58.3816:500}
     * @throws IllegalArgumentException if a coordinate is null, NaN or infinite
     */
    public String create(Double latitude, Double longitude, int radiusMeters)
    {
        requireCoordinateBoxed(latitude, "latitude");
        requireCoordinateBoxed(longitude, "longitude");
        return create(latitude.doubleValue(), longitude.doubleValue(), radiusMeters);
    }

    public String create(double latitude, double longitude, int radiusMeters)
    {
        requireCoordinate(latitude, "latitude");
        requireCoordinate(longitude, "longitude");
        long latRaw = Math.round(latitude * SCALE_FACTOR);
        long lonRaw = Math.round(longitude * SCALE_FACTOR);
        // Canonicalize signed zero
        if (latRaw == 0)
        {
            latRaw = 0;
        }
        if (lonRaw == 0)
        {
            lonRaw = 0;
        }
        StringBuilder sb = new StringBuilder(32);
        sb.append(formatFixed(latRaw)).append(':')
                .append(formatFixed(lonRaw)).append(':')
                .append(radiusMeters);
        return sb.toString();
    }

    private String formatFixed(long scaled)
    {
        boolean negative = scaled < 0;
        long abs = negative ? -scaled : scaled;
        long intPart = abs / (long) SCALE_FACTOR;
        long frac = abs % (long) SCALE_FACTOR;
        StringBuilder sb = new StringBuilder(16);
        if (negative)
        {
            sb.append('-');
        }
        sb.append(intPart).append('.');
        // Pad fraction to exactly 4 digits without String.format allocation
        if (frac < 10L)
        {
            sb.append("000").append(frac);
        }
        else if (frac < 100L)
        {
            sb.append("00").append(frac);
        }
        else if (frac < 1000L)
        {
            sb.append('0').append(frac);
        }
        else
        {
            sb.append(frac);
        }
        return sb.toString();
    }

    private void requireCoordinateBoxed(Double value, String name)
    {
        if (value == null || !Double.isFinite(value.doubleValue()))
        {
            throw new IllegalArgumentException(
                    "Cache key coordinate " + name + " must be finite and non-null, got " + value);
        }
    }

    private void requireCoordinate(double value, String name)
    {
        if (!Double.isFinite(value))
        {
            throw new IllegalArgumentException(
                    "Cache key coordinate " + name + " must be finite, got " + value);
        }
    }
}
