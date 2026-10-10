package com.barboleda.arbolado.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Pre-creates Micrometer counters per operation to avoid per-request lookups.
 */
public class CacheOutageRecorder
{

    private static final String ERRORS_METER = "cache.errors";

    private static final String OPERATION_TAG = "operation";

    private static final String CACHE_TAG = "cache";

    private final Counter getCounter;
    private final Counter putCounter;
    private final Counter evictCounter;
    private final Counter clearCounter;

    public CacheOutageRecorder(MeterRegistry registry, String cacheName)
    {
        this.getCounter = Counter.builder(ERRORS_METER)
                .tag(OPERATION_TAG, "get")
                .tag(CACHE_TAG, cacheName)
                .register(registry);
        this.putCounter = Counter.builder(ERRORS_METER)
                .tag(OPERATION_TAG, "put")
                .tag(CACHE_TAG, cacheName)
                .register(registry);
        this.evictCounter = Counter.builder(ERRORS_METER)
                .tag(OPERATION_TAG, "evict")
                .tag(CACHE_TAG, cacheName)
                .register(registry);
        this.clearCounter = Counter.builder(ERRORS_METER)
                .tag(OPERATION_TAG, "clear")
                .tag(CACHE_TAG, cacheName)
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
