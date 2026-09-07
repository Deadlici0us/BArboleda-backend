package com.barboleda.arbolado.config;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Resolves operation caches through the fail-open decorator.
 *
 * <p>The underlying manager stays auto-configured (bindings intact); only the resolved
 * handles gain fail-open behavior.
 */
public class FailOpenCacheResolver implements CacheResolver
{

    private final CacheManager delegate;

    private final MeterRegistry registry;

    private final ConcurrentHashMap<String, Cache> resolved = new ConcurrentHashMap<>();

    /**
     * Builds the resolver over the auto-configured manager.
     *
     * @param delegate the cache manager to resolve names from
     * @param registry the meter registry for outage counters
     */
    public FailOpenCacheResolver(CacheManager delegate, MeterRegistry registry)
    {
        this.delegate = delegate;
        this.registry = registry;
    }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context)
    {
        return context.getOperation().getCacheNames().stream().map(this::requireCache).toList();
    }

    private Cache requireCache(String name)
    {
        return resolved.computeIfAbsent(name, n ->
        {
            Cache cache = delegate.getCache(n);
            if (cache == null)
            {
                throw new IllegalArgumentException("No cache named " + n);
            }
            return new FailOpenCache(cache, registry);
        });
    }
}
