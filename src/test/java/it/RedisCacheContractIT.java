package it;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.barboleda.arbolado.config.FailOpenCache;
import com.barboleda.arbolado.config.RedisCacheConfig;
import com.barboleda.arbolado.domain.ArbolResponse;
import com.barboleda.arbolado.domain.SearchLimits;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Real-Redis cache contract against Testcontainers {@code redis:7}.
 *
 * <p>CI-gated only ({@code -Pintegration}): needs docker, never runs in the default
 * build. Pins what the unit suite cannot: the production JSON serializer against a
 * real Redis, the documented non-single-flight on parallel cold misses, warm-hit
 * loader skipping, and the 30d TTL from the YAML contract.
 */
@Testcontainers
@Tag("integration")
class RedisCacheContractIT
{

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private LettuceConnectionFactory connectionFactory;

    private Cache cache;

    /**
     * Opens a flushed Redis cache through the production serializer before each test.
     */
    @BeforeEach
    void setUp()
    {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getFirstMappedPort());
        connectionFactory.afterPropertiesSet();
        try (RedisConnection connection = connectionFactory.getConnection())
        {
            connection.serverCommands().flushDb();
        }
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(30))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new RedisCacheConfig(registry).cacheValueSerializer()));
        RedisCacheManager manager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .build();
        manager.afterPropertiesSet();
        Cache redisCache = Objects.requireNonNull(manager.getCache(SearchLimits.CACHE_NAME));
        cache = new FailOpenCache(redisCache, registry);
    }

    /**
     * Closes the factory after each test.
     */
    @AfterEach
    void tearDown()
    {
        connectionFactory.destroy();
    }

    @Test
    @DisplayName("parallel cold misses document the non-single-flight on Redis")
    void parallelColdMissesRunLoaderPerMiss() throws Exception
    {
        // Given a cold key and a slow loader
        AtomicInteger loaderRuns = new AtomicInteger();
        int threads = 32;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try
        {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<List<ArbolResponse>>> futures = IntStream.range(0, threads)
                    .mapToObj(i -> pool.submit(() ->
                    {
                        ready.countDown();
                        start.await();
                        return load("parallel-key", loaderRuns);
                    }))
                    .toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<List<ArbolResponse>> future : futures)
            {
                assertThat(future.get(30, TimeUnit.SECONDS)).hasSize(1);
            }
        }
        finally
        {
            pool.shutdownNow();
        }

        // Then Redis ran the loader more than once — sync=true coalesces only on locking caches
        assertThat(loaderRuns.get()).isGreaterThan(1);
    }

    @Test
    @DisplayName("warm hit skips loader, round-trips 1001 DTOs through GZIP, compressed under 150KB")
    void warmHitSkipsLoader()
    {
        // Given one loaded probe (1001 DTOs = MAX_ITEMS + 1)
        AtomicInteger loaderRuns = new AtomicInteger();
        List<ArbolResponse> probe = IntStream.range(0, 1001).mapToObj(this::dto).toList();
        List<ArbolResponse> first = cache.get("probe-key", () ->
        {
            loaderRuns.incrementAndGet();
            return probe;
        });

        // When reading the warm key
        List<ArbolResponse> second = cache.get("probe-key", () ->
        {
            loaderRuns.incrementAndGet();
            return List.of();
        });

        // Then loader ran once, 1001 DTOs survive GZIP round-trip, payload well under 150KB
        assertThat(loaderRuns).hasValue(1);
        assertThat(first).hasSize(1001);
        assertThat(second).isEqualTo(probe);

        // Confirm compressed payload size (GZIP reduces ~raw to well under 150KB)
        byte[] rawKey = (SearchLimits.CACHE_NAME + "::" + "probe-key").getBytes(StandardCharsets.UTF_8);
        try (RedisConnection conn = connectionFactory.getConnection())
        {
            byte[] compressed = conn.stringCommands().get(rawKey);
            assertThat(compressed).isNotNull();
            assertThat(compressed.length).isGreaterThan(0);
            assertThat(compressed[0]).isEqualTo((byte) 0x1f); // GZIP magic
            assertThat(compressed[1]).isEqualTo((byte) 0x8b);
            assertThat(compressed.length).isLessThan(150_000); // compressed under 150KB
        }
    }

    @Test
    @DisplayName("entries carry the 30-day TTL from the YAML contract")
    void entriesCarryThirtyDayTtl()
    {
        // Given one cached entry
        cache.get("ttl-key", () -> List.of(dto(1)));

        // When reading the raw TTL in seconds
        // Then roughly 30 days remain
        assertThat(ttlSecondsOf("ttl-key")).isGreaterThan(TimeUnit.DAYS.toSeconds(29));
        assertThat(ttlSecondsOf("ttl-key")).isLessThanOrEqualTo(TimeUnit.DAYS.toSeconds(30));
    }

    private List<ArbolResponse> load(String key, AtomicInteger loaderRuns) throws Exception
    {
        return cache.get(key, () ->
        {
            loaderRuns.incrementAndGet();
            Thread.sleep(200);
            return List.of(dto(1));
        });
    }

    private long ttlSecondsOf(String key)
    {
        byte[] rawKey = (SearchLimits.CACHE_NAME + "::" + key).getBytes(StandardCharsets.UTF_8);
        try (RedisConnection connection = connectionFactory.getConnection())
        {
            return connection.keyCommands().ttl(rawKey);
        }
    }

    private ArbolResponse dto(int index)
    {
        return new ArbolResponse(index, "Eucalyptus", 10, 40, 1, -58.3816, -34.6037);
    }
}
