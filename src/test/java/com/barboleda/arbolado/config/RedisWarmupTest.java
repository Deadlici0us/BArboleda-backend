package com.barboleda.arbolado.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Redis warmup contract without any server.
 *
 * <p>The ping shifts the cold TLS cost from the first user request to the
 * deploy, where the readiness probe absorbs it. The cache stays fail-open
 * either way, so this only buys latency, never correctness.
 */
class RedisWarmupTest
{

    @Test
    @DisplayName("warmup pings once and returns the connection")
    void warmupPingsOnce()
    {
        // Given a mocked factory handing out one connection
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(factory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        RedisWarmup warmup = new RedisWarmup(factory);

        // When warming up on boot
        warmup.warmup();

        // Then exactly one ping went through and the connection went back
        verify(factory, times(1)).getConnection();
        verify(connection, times(1)).ping();
        verify(connection, times(1)).close();
    }

    @Test
    @DisplayName("warmup failure never fails the boot")
    void warmupFailureDoesNotPropagate()
    {
        // Given a factory whose connection throws
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        when(factory.getConnection()).thenThrow(new RuntimeException("upstash unreachable"));
        RedisWarmup warmup = new RedisWarmup(factory);

        // When warming up on boot
        // Then the failure is swallowed (the cache path is fail-open anyway)
        assertThatCode(warmup::warmup).doesNotThrowAnyException();
    }
}
