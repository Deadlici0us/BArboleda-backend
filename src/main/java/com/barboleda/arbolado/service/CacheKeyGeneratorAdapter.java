package com.barboleda.arbolado.service;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

/**
 * Programmatic cache key generator: eliminates hidden SpEL dependency.
 * Fixed-radius bucket: key uses only normalized lat/lon (3-decimal).
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
        if (params.length < 2)
        {
            throw new IllegalArgumentException(
                    "CacheKeyGeneratorAdapter expects (latitude, longitude), got "
                            + params.length);
        }
        double latitude = (params[0] instanceof Number)
                ? ((Number) params[0]).doubleValue() : Double.parseDouble(String.valueOf(params[0]));
        double longitude = (params[1] instanceof Number)
                ? ((Number) params[1]).doubleValue() : Double.parseDouble(String.valueOf(params[1]));
        return keyGenerator.create(latitude, longitude);
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
