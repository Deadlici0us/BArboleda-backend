package com.barboleda.arbolado.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.barboleda.arbolado.domain.SearchLimits;

/**
 * Rounds coordinates to {@link SearchLimits#COORDINATE_SCALE} decimals, HALF_UP.
 *
 * <p>Signed zero canonicalizes to {@code 0.0} so {@code -0.0} and {@code 0.0} share
 * one cache key. Non-finite inputs are rejected fast; the service-level finite guard
 * runs first, so this is defense in depth.
 */
@Component
public class RoundingNormalizationStrategy implements CoordinateNormalizationStrategy
{

    @Override
    public Double normalize(Double value)
    {
        if (value == null)
        {
            throw new IllegalArgumentException("Coordinate must not be null");
        }
        if (!Double.isFinite(value))
        {
            throw new IllegalArgumentException("Coordinate must be finite, got " + value);
        }
        double rounded = BigDecimal.valueOf(value)
                .setScale(SearchLimits.COORDINATE_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
        return rounded == 0.0 ? 0.0 : rounded;
    }

    @Override
    public double normalize(double value)
    {
        if (!Double.isFinite(value))
        {
            throw new IllegalArgumentException("Coordinate must be finite, got " + value);
        }
        double rounded = BigDecimal.valueOf(value)
                .setScale(SearchLimits.COORDINATE_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
        return rounded == 0.0 ? 0.0 : rounded;
    }
}
