package com.barboleda.arbolado.config;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;

/**
 * Fail-open decorator for the synchronized cache path.
 *
 * <p>Only {@code get(key, callable)} applies fail-open behavior (the only
 * path that carries a loader). Other methods delegate directly; this keeps
 * the contract consistent with the Spring {@link Cache} interface while
 * avoiding hidden Liskov violations. The underlying cache failure is
 * classified by {@link CacheFailureClassifier} and counted by
 * {@link CacheOutageRecorder} so that loader failures (Mongo 503) propagate
 * untouched and infrastructure outages degrade to direct load.
 */
public class FailOpenCache extends ForwardingCache
{

    private static final Logger log = LoggerFactory.getLogger(FailOpenCache.class);

    private final CacheFailureClassifier classifier;

    private final CacheOutageRecorder recorder;

    /**
     * Wraps one resolved cache with single-flight fail-open behavior.
     *
     * @param delegate the underlying cache, never null
     * @param registry the meter registry for outage counters
     */
    public FailOpenCache(Cache delegate, io.micrometer.core.instrument.MeterRegistry registry)
    {
        super(delegate);
        this.classifier = new CacheFailureClassifier();
        this.recorder = new CacheOutageRecorder(registry, delegate.getName());
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
            // Loader (Mongo/datastore) failure — propagate untouched, no retry,
            // no outage count, so the 503 mapping stays intact.
            throw loaderFailure;
        }
        catch (RuntimeException outage)
        {
            if (classifier.isCacheOutage(outage))
            {
                recorder.recordGet();
                log.warn("Cache {} failed on sync get ({}); failing open to direct load",
                        getName(), outage.toString(), outage);
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
            throw outage;
        }
    }
}
