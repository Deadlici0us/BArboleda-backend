package com.barboleda.arbolado.service;

import com.barboleda.arbolado.domain.SearchLimits;

/**
 * Search radius in whole meters for the DIP port boundary (never Spring types).
 *
 * @param value rounded radius in meters, 1 to {@code MAX_RADIUS_METERS}
 */
public record RadiusMeters(int value)
{

    /**
     * Validates the radius is within allowed bounds before construction.
     *
     * @param value must be between 1 and 1000 inclusive
     */
    public RadiusMeters
    {
        if (value < SearchLimits.MIN_RADIUS_METERS || value > SearchLimits.MAX_RADIUS_METERS)
        {
            throw new IllegalArgumentException(
                    "Radius must be " + SearchLimits.MIN_RADIUS_METERS + ".."
                            + SearchLimits.MAX_RADIUS_METERS + " meters, got " + value);
        }
    }

    /**
     * Converts the radius to kilometers (floating-point division preserved).
     */
    public double toKilometers()
    {
        return value / 1000.0;
    }
}
