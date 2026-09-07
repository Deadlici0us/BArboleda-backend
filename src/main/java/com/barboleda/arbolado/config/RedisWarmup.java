package com.barboleda.arbolado.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Warms the Redis connection once the app is ready.
 */
@Component
@ConditionalOnProperty(name = "app.redis.warmup.enabled", havingValue = "true", matchIfMissing = true)
public class RedisWarmup extends AbstractWarmup
{

    private final RedisConnectionFactory connectionFactory;

    public RedisWarmup(RedisConnectionFactory connectionFactory)
    {
        this.connectionFactory = connectionFactory;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmup()
    {
        timedPing(() ->
        {
            try (RedisConnection connection = connectionFactory.getConnection())
            {
                connection.ping();
            }
        }, "Redis");
    }
}
