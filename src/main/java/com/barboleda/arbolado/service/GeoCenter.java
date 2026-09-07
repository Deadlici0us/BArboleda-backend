package com.barboleda.arbolado.service;

/**
 * Normalized geo search center in lon-first order (x = longitude, y = latitude).
 *
 * @param longitude normalized longitude in degrees, must be finite
 * @param latitude normalized latitude in degrees, must be finite
 */
public record GeoCenter(double longitude, double latitude)
{

    /**
     * Validates the center values before construction.
     *
     * @param longitude must be finite
     * @param latitude must be finite
     */
    public GeoCenter
    {
        if (!Double.isFinite(longitude) || !Double.isFinite(latitude))
        {
            throw new IllegalArgumentException(
                    "GeoCenter requires finite longitude and latitude, got longitude=" + longitude
                            + ", latitude=" + latitude);
        }
    }
}
