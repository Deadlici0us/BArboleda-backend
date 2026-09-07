package com.barboleda.arbolado.config;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;

/**
 * Mongo client adapter: fail-fast timeouts, warm pool and latency listener.
 *
 * <p>Read-only: the app never creates indexes or writes. The {@code 2dsphere}
 * index on {@code location} is owned out-of-band by the ETL/infra
 * ({@code arbolado-db}); {@code auto-index-creation} stays off in YAML.
 *
 * <p>Pool: {@code minSize=1} holds one warm connection across idle gaps on
 * Atlas free tier (0-100 ops/sec bursty); {@code maxIdle=60s} frees burst
 * connections and dodges NAT expiry; {@code maxLife=0} leaves lifetime to the
 * idle bound plus the topology manager.
 */
@Configuration
public class MongoConfig
{

    private final MeterRegistry registry;

    /**
     * Builds the Mongo adapter over the meter registry.
     *
     * @param registry the meter registry for the command latency listener
     */
    public MongoConfig(MeterRegistry registry)
    {
        this.registry = registry;
    }

    /**
     * Builds the driver customizer with fail-fast timeouts, pool bounds and latency metrics.
     *
     * @return the customizer; Boot applies it to the auto-configured client
     */
    @Bean
    public MongoClientSettingsBuilderCustomizer mongoSettingsCustomizer()
    {
        return builder -> builder
                .applyToClusterSettings(cluster -> cluster.serverSelectionTimeout(2500, TimeUnit.MILLISECONDS))
                .applyToSocketSettings(socket -> socket.connectTimeout(2, TimeUnit.SECONDS)
                        .readTimeout(2, TimeUnit.SECONDS))
                .applyToConnectionPoolSettings(pool -> pool
                        .minSize(1)
                        .maxSize(20)
                        .maxWaitTime(5000, TimeUnit.MILLISECONDS)
                        .maxConnectionIdleTime(60000, TimeUnit.MILLISECONDS)
                        .maxConnectionLifeTime(0, TimeUnit.MILLISECONDS))
                .addCommandListener(new MongoMetricsCommandListener(registry));
    }
}
