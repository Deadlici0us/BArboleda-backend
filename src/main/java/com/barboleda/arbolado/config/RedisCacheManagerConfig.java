package com.barboleda.arbolado.config;

import java.util.HashSet;

import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Explicit Redis cache manager: JSON values with YAML-driven defaults.
 *
 * <p>Lives apart from {@link RedisCacheConfig} on purpose: the non-Redis test
 * configs import that class without a connection factory, so a manager bean
 * declared there would need a bean-level condition to survive — and
 * {@code @ConditionalOnBean} on user configuration silently never matches the
 * auto-configured factory (user configs run first), leaving Boot's default JDK
 * manager active ({@code NotSerializableException} on {@code ArbolResponse}).
 * Wherever this class loads, the factory exists, so no such guesswork applies.
 * It also binds {@link CacheProperties} itself: any user-defined
 * {@code cacheManager} bean makes Boot's {@code CacheAutoConfiguration} back off,
 * which would otherwise take the bound properties down with it.
 */
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class RedisCacheManagerConfig
{

    /**
     * Builds the explicit cache manager with the JSON value serializer.
     *
     * @param connectionFactory the Redis connection factory from auto-configuration
     * @param properties the bound {@code spring.cache} properties, TTL source of truth
     * @param valueSerializer the JSON value serializer
     * @return the manager whose values are JSON, never JDK serialization
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, CacheProperties properties,
            RedisSerializer<Object> valueSerializer)
    {
        RedisCacheConfiguration defaults = RedisCacheConfig.redisDefaults(properties, valueSerializer);
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .initialCacheNames(new HashSet<>(properties.getCacheNames()))
                .build();
    }
}
