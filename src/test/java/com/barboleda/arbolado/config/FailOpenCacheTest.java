package com.barboleda.arbolado.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import com.barboleda.arbolado.domain.SearchLimits;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Fail-open discrimination contract for {@link FailOpenCache} (PLAN.md #5, #11).
 *
 * <p>A {@code Cache.ValueRetrievalException} means the loader (Mongo) failed — it
 * must propagate untouched, with no retry and no outage count, so the 503 mapping
 * keeps working. Any other {@code RuntimeException} means the cache (Redis) failed —
 * fail open by loading directly instead.
 */
class FailOpenCacheTest
{

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Test
    @DisplayName("loader failure propagates untouched with no retry and no outage count")
    void loaderFailurePropagatesUntouched()
    {
        // Given a delegate that runs the loader and wraps its failure, exactly like
        // RedisCache does on a Mongo outage
        AtomicInteger loaderRuns = new AtomicInteger();
        Callable<String> loader = countingLoader(loaderRuns, "mongo-data");
        Cache.ValueRetrievalException wrapped = new Cache.ValueRetrievalException("k", loader,
                new RuntimeException("mongo down"));
        FailOpenCache cache = new FailOpenCache(new ScriptedCache(invocation ->
        {
            throw wrapped;
        }), registry);

        // When getting
        // Then the wrapper escapes untouched: no second loader run, no outage counted
        assertThatThrownBy(() -> cache.get("k", loader)).isSameAs(wrapped);
        assertThat(loaderRuns).hasValue(0);
        assertThat(outageCount()).isZero();
    }

    @Test
    @DisplayName("sync-path outage loads directly, skips the store and counts every time")
    void syncOutageFallsBackToLoader()
    {
        // Given a dead underlying cache and a loader that ran zero times
        AtomicInteger loaderRuns = new AtomicInteger();
        FailOpenCache cache = new FailOpenCache(new ScriptedCache(invocation ->
        {
            throw new RuntimeException("Redis down");
        }), registry);

        // When loading through the decorator twice
        assertThat(cache.get("k", countingLoader(loaderRuns, "fresh"))).isEqualTo("fresh");
        assertThat(cache.get("k", countingLoader(loaderRuns, "fresh"))).isEqualTo("fresh");

        // Then the loader ran every time, nothing stuck, and each outage counted
        assertThat(loaderRuns).hasValue(2);
        assertThat(outageCount()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("put failure inside the sync get still fails open with the loader value")
    void putFailureInsideGetFailsOpen()
    {
        // Given a delegate that loads once and then fails the write, like a Redis
        // put failure inside RedisCache.get(key, callable)
        FailOpenCache cache = new FailOpenCache(new ScriptedCache(invocation ->
        {
            try
            {
                invocation.call();
            }
            catch (Exception loaderFailure)
            {
                throw new IllegalStateException(loaderFailure);
            }
            throw new RuntimeException("Redis put down");
        }), registry);

        // When getting
        // Then the loaded value still serves and the outage counts once
        assertThat(cache.get("k", () -> "direct")).isEqualTo("direct");
        assertThat(outageCount()).isOne();
    }

    @Test
    @DisplayName("sync-path outage warn carries the failure cause for diagnosis")
    void syncOutageWarnCarriesCause()
    {
        // Given a dead underlying cache and a captured log
        Logger logger = (Logger) LoggerFactory.getLogger(FailOpenCache.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try
        {
            FailOpenCache cache = new FailOpenCache(new ScriptedCache(invocation ->
            {
                throw new RuntimeException("Redis down");
            }), registry);

            // When loading through the decorator
            assertThat(cache.get("k", () -> "fresh")).isEqualTo("fresh");

            // Then the warn names the cache and carries the cause, so timeout vs
            // serialization vs connection failures stay distinguishable in prod
            assertThat(appender.list).anySatisfy(event ->
            {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).contains(SearchLimits.CACHE_NAME);
                assertThat(event.getThrowableProxy()).isNotNull();
                assertThat(event.getThrowableProxy().getMessage()).contains("Redis down");
            });
        }
        finally
        {
            logger.detachAppender(appender);
        }
    }

    @Test
    @DisplayName("healthy delegate passes through with no counting")
    void healthyDelegates()
    {
        // Given a working cache holding one entry
        ConcurrentMapCache delegate = new ConcurrentMapCache(SearchLimits.CACHE_NAME);
        delegate.put("k", "v");
        FailOpenCache cache = new FailOpenCache(delegate, registry);

        // When reading simply and through the loader
        // Then values pass through and no outage counts
        assertThat(cache.get("k").get()).isEqualTo("v");
        assertThat(cache.get("missing", () -> "loaded")).isEqualTo("loaded");
        assertThat(outageCount()).isZero();
    }

    private double outageCount()
    {
        return registry.counter("cache.errors", "operation", "get", "cache", SearchLimits.CACHE_NAME).count();
    }

    private static Callable<String> countingLoader(AtomicInteger runs, String value)
    {
        return () ->
        {
            runs.incrementAndGet();
            return value;
        };
    }

    /**
     * Cache stub with a scripted synchronized-get behavior; every other op is unused.
     */
    static final class ScriptedCache implements Cache
    {

        private final Function<Callable<?>, Object> behavior;

        ScriptedCache(Function<Callable<?>, Object> behavior)
        {
            this.behavior = behavior;
        }

        @Override
        public String getName()
        {
            return SearchLimits.CACHE_NAME;
        }

        @Override
        public Object getNativeCache()
        {
            return new Object();
        }

        @Override
        public ValueWrapper get(Object key)
        {
            throw new UnsupportedOperationException("unused");
        }

        @Override
        public <T> T get(Object key, Class<T> type)
        {
            throw new UnsupportedOperationException("unused");
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Callable<T> valueLoader)
        {
            return (T) behavior.apply(valueLoader);
        }

        @Override
        public void put(Object key, Object value)
        {
            throw new UnsupportedOperationException("unused");
        }

        @Override
        public void evict(Object key)
        {
            throw new UnsupportedOperationException("unused");
        }

        @Override
        public void clear()
        {
            throw new UnsupportedOperationException("unused");
        }
    }
}
