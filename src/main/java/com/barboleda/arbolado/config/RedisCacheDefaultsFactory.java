package com.barboleda.arbolado.config;

import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Factory that builds Redis cache defaults outside of the adapter config,
 * so that tests can call it without pulling in a connection factory.
 */
public class RedisCacheDefaultsFactory
{

    public static RedisCacheConfiguration buildDefaults(CacheProperties properties,
            RedisSerializer<Object> valueSerializer)
    {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.getRedis().getTimeToLive())
                .serializeValuesWith(
                        org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                                .fromSerializer(valueSerializer));
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
}
