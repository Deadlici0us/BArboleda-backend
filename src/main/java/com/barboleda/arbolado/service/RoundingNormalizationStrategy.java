package com.barboleda.arbolado.service;

import org.springframework.stereotype.Component;

import com.barboleda.arbolado.domain.SearchLimits;

/**
 * Rounds coordinates to 3-decimal grid (~111m). Used for fixed 1000m bucket keys.
 *
 * <p>Uses scaled-long arithmetic (no {@code BigDecimal}). Signed zero canonicalizes.
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
