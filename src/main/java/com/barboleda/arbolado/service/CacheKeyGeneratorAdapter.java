package com.barboleda.arbolado.service;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

/**
 * Programmatic cache key generator: eliminates the hidden SpEL string dependency
 * in {@code @Cacheable} by computing the key directly from method arguments.
 */
@Component("cacheKeyGenerator")
public class CacheKeyGeneratorAdapter implements KeyGenerator
{

    private final CacheKeyFactory keyFactory;

    public CacheKeyGeneratorAdapter(CacheKeyFactory keyFactory)
    {
        this.keyFactory = keyFactory;
    }

    @Override
    public Object generate(Object target, Method method, Object... params)
    {
        if (params.length < 3)
        {
            throw new IllegalArgumentException(
                    "CacheKeyGeneratorAdapter expects (latitude, longitude, radiusMeters), got "
                            + params.length);
        }
        double latitude = (Double) params[0];
        double longitude = (Double) params[1];
        int radiusMeters = (Integer) params[2];
        return keyFactory.create(latitude, longitude, radiusMeters);
    }

    @Override
    public int hashCode()
    {
        return CacheKeyGeneratorAdapter.class.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof CacheKeyGeneratorAdapter;
    }
}
