package com.barboleda.arbolado.service;

import org.springframework.stereotype.Component;

import com.barboleda.arbolado.domain.SearchLimits;

/**
 * Rounds coordinates to {@link SearchLimits#COORDINATE_SCALE} decimals, HALF_UP.
 *
 * <p>Uses scaled-long arithmetic (no {@code BigDecimal} allocation) on the hot
 * path. Signed zero canonicalizes to {@code 0.0} so {@code -0.0} and {@code 0.0}
 * share one cache key. Non-finite inputs are rejected fast.
 */
@Component
public class RoundingNormalizationStrategy implements CoordinateNormalizationStrategy
{

    private static final int SCALE = SearchLimits.COORDINATE_SCALE;
    private static final double SCALE_FACTOR = Math.pow(10.0, SCALE);

    @Override
    public double normalize(double value)
    {
        if (!Double.isFinite(value))
        {
            throw new IllegalArgumentException("Coordinate must be finite, got " + value);
        }
        long scaled = Math.round(Math.abs(value) * SCALE_FACTOR);
        double rounded = (value < 0 ? -scaled : scaled) / SCALE_FACTOR;
        return rounded == 0.0 ? 0.0 : rounded;
    }
}
