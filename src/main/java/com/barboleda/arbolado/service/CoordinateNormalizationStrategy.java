package com.barboleda.arbolado.service;

/**
 * Normalizes a coordinate onto the fixed grid used for cache keys and queries.
 *
 * <p>OCP: a new precision is a new implementation, never an edit.
 */
public interface CoordinateNormalizationStrategy
{

    /**
     * Normalizes one coordinate value.
     *
     * @param value the raw coordinate, must be finite and non-null
     * @return the grid-aligned coordinate
     * @throws IllegalArgumentException if {@code value} is null, NaN or infinite
     */
    Double normalize(Double value);
}
