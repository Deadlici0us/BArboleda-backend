package com.barboleda.arbolado.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;

import com.barboleda.arbolado.config.RedisCacheConfig;
import com.barboleda.arbolado.domain.Arbol;
import com.barboleda.arbolado.domain.SearchLimits;
import com.barboleda.arbolado.domain.SearchRequest;
import com.barboleda.arbolado.domain.SearchResponse;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Proves the cache proxy applies: the cached bean must be separate, never a self-call.
 */
@SpringBootTest(classes = CacheProxyTest.ProxyConfig.class, properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"})
class CacheProxyTest
{

    @MockBean
    private ArbolSearchPort port;

    @Autowired
    private ArbolService service;

    @Test
    @DisplayName("second identical normalized call hits the cache, not the port")
    void secondCallHitsCache()
    {
        // Given one stored tree
        Arbol entity = new Arbol("x", null, 1, "Eucalyptus", 10, 40, 1, -58.3816, -34.6037, "csv", false);
        when(port.searchNear(any(Point.class), any(Distance.class))).thenReturn(List.of(entity));

        // When searching twice with the same normalized inputs
        SearchRequest request = new SearchRequest(-34.6037, -58.3816, 500.0);
        SearchResponse first = service.findNearby(request);
        SearchResponse second = service.findNearby(request);

        // Then the port ran once and both responses match
        verify(port, times(1)).searchNear(any(Point.class), any(Distance.class));
        assertThat(first).isEqualTo(second);
        assertThat(first.items()).hasSize(1);
    }

    @Test
    @DisplayName("32 parallel cold misses single-flight to one port call")
    void parallelColdMissesSingleFlight() throws Exception
    {
        // Given one stored tree behind an empty cache and a slow port
        Arbol entity = new Arbol("x", null, 1, "Eucalyptus", 10, 40, 1, -58.3816, -34.6037, "csv", false);
        when(port.searchNear(any(Point.class), any(Distance.class))).thenAnswer(invocation ->
        {
            Thread.sleep(50);
            return List.of(entity);
        });

        // When 32 threads search the same inputs at once (a cold key no other test touches)
        int threads = 32;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try
        {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch start = new CountDownLatch(1);
            SearchRequest request = new SearchRequest(-34.7000, -58.5000, 250.0);
            List<Future<SearchResponse>> futures = IntStream.range(0, threads).mapToObj(i -> pool.submit(() ->
            {
                ready.countDown();
                start.await();
                return service.findNearby(request);
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<SearchResponse> future : futures)
            {
                assertThat(future.get(30, TimeUnit.SECONDS).items()).hasSize(1);
            }
        }
        finally
        {
            pool.shutdownNow();
        }

        // Then the port ran exactly once — the locking cache coalesced the miss
        verify(port, times(1)).searchNear(any(Point.class), any(Distance.class));
    }

    /**
     * Minimal web-free context: real service beans plus a simple cache manager.
     */
    @Configuration
    @EnableCaching
    @Import(RedisCacheConfig.class)
    static class ProxyConfig
    {

        /**
         * Builds the service facade from injected collaborators.
         *
         * @param normalizer the coordinate grid strategy
         * @param cachedSearch the cache-owning search bean
         * @return the facade
         */
        @Bean
        ArbolService arbolService(CoordinateNormalizationStrategy normalizer, CachedArbolSearch cachedSearch)
        {
            return new ArbolService(normalizer, cachedSearch);
        }

        /**
         * Builds the cached search from injected collaborators.
         *
         * @param port the mocked geo search port
         * @param queryFactory the lon-first query factory
         * @param mapper the entity-to-DTO mapper
         * @return the cached search
         */
        @Bean
        CachedArbolSearch cachedArbolSearch(ArbolSearchPort port, GeoQueryFactory queryFactory, ArbolMapper mapper)
        {
            return new CachedArbolSearch(port, queryFactory, mapper);
        }

        /**
         * Builds the normalization strategy.
         *
         * @return the rounding strategy
         */
        @Bean
        CoordinateNormalizationStrategy normalizer()
        {
            return new RoundingNormalizationStrategy();
        }

        /**
         * Builds the query factory.
         *
         * @return the factory
         */
        @Bean
        GeoQueryFactory queryFactory()
        {
            return new GeoQueryFactory();
        }

        /**
         * Builds the mapper.
         *
         * @return the mapper
         */
        @Bean
        ArbolMapper mapper()
        {
            return new ArbolMapper();
        }

        /**
         * Builds the key factory behind the cache SpEL reference.
         *
         * @return the factory
         */
        @Bean
        CacheKeyFactory cacheKeyFactory()
        {
            return new CacheKeyFactory();
        }

        /**
         * Builds an in-memory cache manager standing in for Redis.
         *
         * @return the cache manager
         */
        @Bean
        CacheManager cacheManager()
        {
            SimpleCacheManager manager = new SimpleCacheManager();
            manager.setCaches(List.of(new ConcurrentMapCache(SearchLimits.CACHE_NAME)));
            return manager;
        }

        /**
         * Builds a no-op meter registry for the context.
         *
         * @return the registry
         */
        @Bean
        MeterRegistry meterRegistry()
        {
            return new SimpleMeterRegistry();
        }
    }
}
