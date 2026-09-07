package com.barboleda.arbolado.config;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Fail-open decorator for the synchronized cache path.
 *
 * <p>Spring routes raw failures from {@code get(key, callable)} past the
 * {@code CacheErrorHandler}, so a Redis outage on a {@code sync} entry would escape
 * as an error instead of falling back. This decorator catches the outage, counts it,
 * logs it with its cause (timeout vs serialization vs connection), and loads directly
 * without caching. A {@code Cache.ValueRetrievalException} means
 * the loader (Mongo) failed — never the cache — so it propagates untouched with no
 * retry and no outage count, keeping the 503 mapping working.
 */
public class FailOpenCache implements Cache
{

    private static final Logger log = LoggerFactory.getLogger(FailOpenCache.class);

    private final Cache delegate;

    private final MeterRegistry registry;

    /**
     * Wraps one resolved cache with single-flight fail-open behavior.
     *
     * @param delegate the underlying cache, never null
     * @param registry the meter registry for outage counters
     */
    public FailOpenCache(Cache delegate, MeterRegistry registry)
    {
        this.delegate = delegate;
        this.registry = registry;
    }

    @Override
    public String getName()
    {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache()
    {
        return delegate.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key)
    {
        return delegate.get(key);
    }

    @Override
    public <T> T get(Object key, Class<T> type)
    {
        return delegate.get(key, type);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader)
    {
        try
        {
            return delegate.get(key, valueLoader);
        }
        catch (Cache.ValueRetrievalException loaderFailure)
        {
            throw loaderFailure;
        }
        catch (RuntimeException outage)
        {
            registry.counter("cache.errors", "operation", "get", "cache", getName()).increment();
            log.warn("Cache {} failed on sync get ({}); failing open", getName(), outage.toString(), outage);
            try
            {
                return valueLoader.call();
            }
            catch (RuntimeException loaderFailure)
            {
                throw loaderFailure;
            }
            catch (Exception loaderFailure)
            {
                throw new Cache.ValueRetrievalException(key, valueLoader, loaderFailure);
            }
        }
    }

    @Override
    public void put(Object key, Object value)
    {
        delegate.put(key, value);
    }

    @Override
    public void evict(Object key)
    {
        delegate.evict(key);
    }

    @Override
    public void clear()
    {
        delegate.clear();
    }
}
