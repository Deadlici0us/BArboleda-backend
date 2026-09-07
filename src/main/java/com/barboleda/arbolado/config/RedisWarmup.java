package com.barboleda.arbolado.config;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Warms the Redis connection once the app is ready.
 *
 * <p>Lettuce connects lazily over a single shared connection, so the first
 * cache command after boot pays the TCP + TLS handshake (measured 473ms to
 * Upstash). This ping shifts that cost to the deploy. Fail-open: a failed
 * ping only logs, the cache path degrades to Mongo either way.
 */
@Component
@ConditionalOnProperty(name = "app.redis.warmup.enabled", havingValue = "true", matchIfMissing = true)
public class RedisWarmup
{

    private static final Logger log = LoggerFactory.getLogger(RedisWarmup.class);

    private final RedisConnectionFactory connectionFactory;

    /**
     * Builds the warmup over the shared connection factory.
     *
     * @param connectionFactory the auto-configured factory, never null
     */
    public RedisWarmup(RedisConnectionFactory connectionFactory)
    {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Pings Redis once the context is ready so the shared connection is live.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmup()
    {
        long start = System.nanoTime();
        try (RedisConnection connection = connectionFactory.getConnection())
        {
            connection.ping();
            long rttMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            log.info("Redis warmup ping completed rtt_ms={}", rttMs);
        }
        catch (RuntimeException failure)
        {
            log.warn("Redis warmup ping failed ({}); cache stays fail-open", failure.toString());
        }
    }
}
