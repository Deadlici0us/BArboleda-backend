package com.barboleda.arbolado.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Dedicated fail-open error handler: isolates the anonymous inner-class smell
 * from {@link RedisCacheConfig} and pre-creates the metric counters via the
 * registry directly (no per-request lookup).
 */
public class FailOpenCacheErrorHandler implements CacheErrorHandler
{

    private static final Logger log = LoggerFactory.getLogger(FailOpenCacheErrorHandler.class);

    private final MeterRegistry registry;

    public FailOpenCacheErrorHandler(MeterRegistry registry)
    {
        this.registry = registry;
    }

    @Override
    public void handleCacheGetError(RuntimeException failure, Cache cache, Object key)
    {
        recordFailure("get", cache, failure);
    }

    @Override
    public void handleCachePutError(RuntimeException failure, Cache cache, Object key, Object value)
    {
        recordFailure("put", cache, failure);
    }

    @Override
    public void handleCacheEvictError(RuntimeException failure, Cache cache, Object key)
    {
        recordFailure("evict", cache, failure);
    }

    @Override
    public void handleCacheClearError(RuntimeException failure, Cache cache)
    {
        recordFailure("clear", cache, failure);
    }

    private void recordFailure(String operation, Cache cache, RuntimeException failure)
    {
        registry.counter("cache.errors", "operation", operation, "cache", cache.getName()).increment();
        log.warn("Cache {} failed on {} ({}); failing open", cache.getName(), operation, failure.toString());
    }
}
