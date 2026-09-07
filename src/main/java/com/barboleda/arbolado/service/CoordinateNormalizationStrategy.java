package com.barboleda.arbolado.service;

/**
 * Normalizes a coordinate onto the fixed grid used for cache keys and queries.
 *
 * <p>OCP: a new precision is a new implementation, never an edit.
 */
public interface CoordinateNormalizationStrategy
{

    /**
     * Normalizes one coordinate value (primitive, hot path, no autoboxing).
     *
     * @param value the raw coordinate, must be finite
     * @return the grid-aligned coordinate
     * @throws IllegalArgumentException if NaN or infinite
     */
    double normalize(double value);

    /**
     * Boxed overload that delegates to the primitive form.
     *
     * @param value the raw coordinate, must be finite and non-null
     * @return the grid-aligned coordinate
     * @throws IllegalArgumentException if null, NaN or infinite
     */
    default Double normalize(Double value)
    {
        if (value == null || !Double.isFinite(value))
        {
            throw new IllegalArgumentException(
                    "Coordinate must be finite and non-null, got " + value);
        }
        return normalize(value.doubleValue());
    }
}
