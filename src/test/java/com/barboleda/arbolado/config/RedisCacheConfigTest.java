package com.barboleda.arbolado.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.barboleda.arbolado.domain.ArbolResponse;
import com.barboleda.arbolado.domain.SearchLimits;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Redis cache wiring contract without any server (PLAN.md #2, #8).
 */
class RedisCacheConfigTest
{

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private final RedisCacheConfig config = new RedisCacheConfig(registry);

    @Test
    @DisplayName("record DTO list round-trips through the configured value serializer")
    void serializerRoundTripsRecordList()
    {
        // Given the EVERYTHING-typed JSON value serializer
        RedisSerializer<Object> serializer = config.cacheValueSerializer();
        List<ArbolResponse> dtos = List.of(new ArbolResponse(123, "Jacaranda mimosifolia", 8, 30, 1, -58.3816,
                -34.6037));

        // When round-tripping a probe-sized payload
        Object back = serializer.deserialize(serializer.serialize(dtos));

        // Then records survive final-class typing (NON_FINAL alone would break this)
        assertThat(back).isEqualTo(dtos);
    }

    @Test
    @DisplayName("all-null DTO round-trips through the configured value serializer")
    void serializerRoundTripsNullFields()
    {
        // Given a DTO with every field null
        RedisSerializer<Object> serializer = config.cacheValueSerializer();
        List<ArbolResponse> dtos = List.of(new ArbolResponse(0, "", 0, 0, 0, 0.0, 0.0));

        // When round-tripping
        Object back = serializer.deserialize(serializer.serialize(dtos));

        // Then nulls survive instead of breaking the entry
        assertThat(back).isEqualTo(dtos);
    }

    @Test
    @DisplayName("error handler fails open on every operation and counts it")
    void errorHandlerFailsOpen()
    {
        // Given a Redis outage behind every cache operation
        RuntimeException outage = new RuntimeException("Redis down");
        Cache cache = new ConcurrentMapCache("arboles");

        // When handling each failure
        // Then nothing escapes and each operation counts once
        assertThatNoException().isThrownBy(() -> config.errorHandler().handleCacheGetError(outage, cache, "k"));
        assertThatNoException()
                .isThrownBy(() -> config.errorHandler().handleCachePutError(outage, cache, "k", "v"));
        assertThatNoException().isThrownBy(() -> config.errorHandler().handleCacheEvictError(outage, cache, "k"));
        assertThatNoException().isThrownBy(() -> config.errorHandler().handleCacheClearError(outage, cache));
        assertThat(registry.counter("cache.errors", "operation", "get", "cache", "arboles").count()).isOne();
        assertThat(registry.counter("cache.errors", "operation", "put", "cache", "arboles").count()).isOne();
        assertThat(registry.counter("cache.errors", "operation", "evict", "cache", "arboles").count()).isOne();
        assertThat(registry.counter("cache.errors", "operation", "clear", "cache", "arboles").count()).isOne();
    }

    @Test
    @DisplayName("cache defaults stay bound to the YAML values")
    void defaultsComeFromYamlBindings()
    {
        // Given the bound properties mirroring application.yml (no key-prefix: the
        // default already namespaces keys as arboles::<key>)
        CacheProperties properties = new CacheProperties();
        properties.getRedis().setTimeToLive(Duration.ofDays(30));
        properties.getRedis().setCacheNullValues(false);
        properties.getRedis().setUseKeyPrefix(true);

        // When building the manager defaults
        RedisCacheConfiguration defaults = RedisCacheConfig.redisDefaults(properties, config.cacheValueSerializer());

        // Then TTL, no-nulls and the default name prefix match the YAML contract
        assertThat(defaults.getTtl()).isEqualTo(Duration.ofDays(30));
        assertThat(defaults.getAllowCacheNullValues()).isFalse();
        assertThat(defaults.getKeyPrefixFor("arboles")).isEqualTo("arboles::");
        assertThat(defaults.getValueSerializationPair()).isNotNull();
    }

    @Test
    @DisplayName("explicit manager serves JSON values, never JDK serialization")
    void managerUsesJsonValues()
    {
        // Given the bound properties mirroring application.yml plus the cache name
        CacheProperties properties = new CacheProperties();
        properties.getRedis().setTimeToLive(Duration.ofDays(30));
        properties.getRedis().setCacheNullValues(false);
        properties.getRedis().setUseKeyPrefix(true);
        properties.getCacheNames().add(SearchLimits.CACHE_NAME);
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);

        // When building the explicit manager
        CacheManager manager = new RedisCacheManagerConfig().cacheManager(factory, properties,
                config.cacheValueSerializer());

        // Then the cache materializes through the manager with YAML TTL, no-nulls and prefix intact
        RedisCache cache = (RedisCache) manager.getCache(SearchLimits.CACHE_NAME);
        assertThat(cache).isNotNull();
        assertThat(manager.getCacheNames()).contains(SearchLimits.CACHE_NAME);
        RedisCacheConfiguration effective = cache.getCacheConfiguration();
        assertThat(effective.getTtl()).isEqualTo(Duration.ofDays(30));
        assertThat(effective.getAllowCacheNullValues()).isFalse();
        assertThat(effective.getKeyPrefixFor(SearchLimits.CACHE_NAME)).isEqualTo("arboles::");

        // And record DTOs round-trip through the effective pair (JDK would throw NotSerializableException)
        List<ArbolResponse> dtos = List.of(new ArbolResponse(123, "Jacaranda mimosifolia", 8, 30, 1, -58.3816,
                -34.6037));
        ByteBuffer raw = effective.getValueSerializationPair().write(dtos);
        assertThat(effective.getValueSerializationPair().read(raw)).isEqualTo(dtos);
    }
}
