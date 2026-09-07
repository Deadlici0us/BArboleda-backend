package com.barboleda.arbolado.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Redis cache adapter: JSON value serializer, YAML-driven defaults, fail-open errors.
 *
 * <p>The explicit JSON cache manager lives in {@link RedisCacheManagerConfig},
 * deliberately separate: the non-Redis test configs import this class without a
 * connection factory, so no bean here may depend on one.
 *
 * <p>Hit/miss counters come from Boot's cache-metrics auto-configuration (actuator on
 * the classpath) — micrometer-core ships no generic cache wrapper, so no custom
 * metrics code lives here.
 */
@Configuration
public class RedisCacheConfig implements CachingConfigurer
{

    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

    private final MeterRegistry registry;

    /**
     * Builds the cache adapter over the meter registry.
     *
     * @param registry the meter registry for fail-open counters
     */
    public RedisCacheConfig(MeterRegistry registry)
    {
        this.registry = registry;
    }

    /**
     * Builds the JSON value serializer for cached DTOs.
     *
     * @return the serializer with EVERYTHING typing so final record DTOs round-trip
     */
    @Bean
    public RedisSerializer<Object> cacheValueSerializer()
    {
        ObjectMapper mapper = new ObjectMapper();
        PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        mapper.activateDefaultTyping(validator, ObjectMapper.DefaultTyping.EVERYTHING);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    /**
     * Derives manager defaults from the bound YAML properties.
     *
     * @param properties the bound {@code spring.cache} properties, TTL source of truth
     * @param valueSerializer the JSON value serializer
     * @return the defaults with YAML TTL, null and prefix rules applied
     */
    static RedisCacheConfiguration redisDefaults(CacheProperties properties, RedisSerializer<Object> valueSerializer)
    {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.getRedis().getTimeToLive())
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));
        if (!properties.getRedis().isCacheNullValues())
        {
            defaults = defaults.disableCachingNullValues();
        }
        if (properties.getRedis().isUseKeyPrefix() && properties.getRedis().getKeyPrefix() != null)
        {
            defaults = defaults.prefixCacheNameWith(properties.getRedis().getKeyPrefix());
        }
        return defaults;
    }

    /**
     * Builds the fail-open resolver over the explicit manager.
     *
     * @param cacheManager the explicit JSON cache manager, bindings intact
     * @param registry the meter registry for outage counters
     * @return the resolver referenced by the cached search
     */
    @Bean("failOpenCacheResolver")
    public FailOpenCacheResolver failOpenCacheResolver(CacheManager cacheManager, MeterRegistry registry)
    {
        return new FailOpenCacheResolver(cacheManager, registry);
    }

    @Override
    public CacheErrorHandler errorHandler()
    {
        return new CacheErrorHandler()
        {
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
        };
    }

    private void recordFailure(String operation, Cache cache, RuntimeException failure)
    {
        registry.counter("cache.errors", "operation", operation, "cache", cache.getName()).increment();
        log.warn("Cache {} failed on {} ({}); failing open", cache.getName(), operation, failure.toString());
    }
}
