package com.barboleda.arbolado.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Pre-creates Micrometer counters per operation to avoid per-request lookups.
 */
public class CacheOutageRecorder
{

    private final Counter getCounter;
    private final Counter putCounter;
    private final Counter evictCounter;
    private final Counter clearCounter;

    public CacheOutageRecorder(MeterRegistry registry, String cacheName)
    {
        this.getCounter = Counter.builder("cache.errors")
                .tag("operation", "get")
                .tag("cache", cacheName)
                .register(registry);
        this.putCounter = Counter.builder("cache.errors")
                .tag("operation", "put")
                .tag("cache", cacheName)
                .register(registry);
        this.evictCounter = Counter.builder("cache.errors")
                .tag("operation", "evict")
                .tag("cache", cacheName)
                .register(registry);
        this.clearCounter = Counter.builder("cache.errors")
                .tag("operation", "clear")
                .tag("cache", cacheName)
                .register(registry);
    }

    public void recordGet()
    {
        getCounter.increment();
    }

    public void recordPut()
    {
        putCounter.increment();
    }

    public void recordEvict()
    {
        evictCounter.increment();
    }

    public void recordClear()
    {
        clearCounter.increment();
    }
}
