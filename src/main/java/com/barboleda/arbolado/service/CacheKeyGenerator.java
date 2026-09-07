package com.barboleda.arbolado.service;

/**
 * Abstraction for generating fixed-precision cache keys (DIP: hides SpEL string dependency).
 */
public interface CacheKeyGenerator
{

    /**
     * Creates the cache key from normalized inputs.
     *
     * @param latitude normalized latitude
     * @param longitude normalized longitude
     * @param radiusMeters rounded radius in whole meters
     * @return the key string
     */
    String create(double latitude, double longitude, int radiusMeters);
}
