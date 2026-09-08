package com.barboleda.arbolado.service;

/**
 * Abstraction for generating fixed-precision cache keys (DIP: hides SpEL string dependency).
 */
public interface CacheKeyGenerator
{

    /**
     * Creates the cache key from normalized coordinates (fixed-radius bucket, no radius segment).
     *
     * @param latitude normalized latitude
     * @param longitude normalized longitude
     * @return the key string
     */
    String create(double latitude, double longitude);
}
