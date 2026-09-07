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

    private final CacheKeyGenerator keyGenerator;

    public CacheKeyGeneratorAdapter(CacheKeyGenerator keyGenerator)
    {
        this.keyGenerator = keyGenerator;
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
        double latitude = (params[0] instanceof Number)
                ? ((Number) params[0]).doubleValue() : Double.parseDouble(String.valueOf(params[0]));
        double longitude = (params[1] instanceof Number)
                ? ((Number) params[1]).doubleValue() : Double.parseDouble(String.valueOf(params[1]));
        int radiusMeters = (params[2] instanceof RadiusMeters) ? ((RadiusMeters) params[2]).value()
                : ((Number) params[2]).intValue();
        return keyGenerator.create(latitude, longitude, radiusMeters);
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
