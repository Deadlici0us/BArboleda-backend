package com.barboleda.arbolado.service;

import org.springframework.stereotype.Component;

/**
 * Builds fixed-precision cache keys from normalized coordinates (3-decimal grid, no radius segment).
 *
 * <p>A Spring bean so the {@code @Cacheable} key adapter resolves by name. Key format is
 * {@code lat:lon}, e.g. {@code -34.604:-58.382}. Fixed 1000m bucket is implicit.
 */
@Component
public class CacheKeyFactory implements CacheKeyGenerator
{

    private static final int SCALE = 3;
    private static final double SCALE_FACTOR = 1000.0;

    @Override
    public String create(double latitude, double longitude)
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
        StringBuilder sb = new StringBuilder(24);
        sb.append(formatFixed(latRaw)).append(':')
                .append(formatFixed(lonRaw));
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
        // Pad fraction to exactly 3 digits without String.format
        if (frac < 10L)
        {
            sb.append("00").append(frac);
        }
        else if (frac < 100L)
        {
            sb.append('0').append(frac);
        }
        else
        {
            sb.append(frac);
        }
        return sb.toString();
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
