package com.barboleda.arbolado.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoClientSettings;

import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Mongo client wiring contract without any server (PLAN.md #2).
 *
 * <p>Read-only: the app owns no index creation; the {@code 2dsphere} index lives
 * out-of-band with the ETL/infra.
 */
class MongoConfigTest
{

    private final MongoConfig config = new MongoConfig(new SimpleMeterRegistry());

    @Test
    @DisplayName("client customizer applies fast timeouts and the latency listener")
    void customizerAppliesTimeoutsAndListener()
    {
        // Given a fresh client settings builder
        MongoClientSettings.Builder builder = MongoClientSettings.builder();

        // When applying the customizer
        config.mongoSettingsCustomizer().customize(builder);
        MongoClientSettings settings = builder.build();

        // Then fail-fast timeouts and the Micrometer listener are in place
        assertThat(settings.getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS)).isEqualTo(2000);
        assertThat(settings.getSocketSettings().getReadTimeout(TimeUnit.MILLISECONDS)).isEqualTo(2000);
        assertThat(settings.getClusterSettings().getServerSelectionTimeout(TimeUnit.MILLISECONDS)).isEqualTo(2500);
        assertThat(settings.getCommandListeners()).anyMatch(MongoMetricsCommandListener.class::isInstance);
    }

    @Test
    @DisplayName("client customizer applies warm pool settings for low-traffic Atlas")
    void customizerAppliesPoolSettings()
    {
        // Given a fresh client settings builder
        MongoClientSettings.Builder builder = MongoClientSettings.builder();

        // When applying the customizer
        config.mongoSettingsCustomizer().customize(builder);
        MongoClientSettings settings = builder.build();

        // Then one warm connection is held with bursty-traffic bounds
        assertThat(settings.getConnectionPoolSettings().getMinSize()).isEqualTo(1);
        assertThat(settings.getConnectionPoolSettings().getMaxSize()).isEqualTo(20);
        assertThat(settings.getConnectionPoolSettings().getMaxWaitTime(TimeUnit.MILLISECONDS)).isEqualTo(5000);
        assertThat(settings.getConnectionPoolSettings().getMaxConnectionIdleTime(TimeUnit.MILLISECONDS))
                .isEqualTo(60000);
        assertThat(settings.getConnectionPoolSettings().getMaxConnectionLifeTime(TimeUnit.MILLISECONDS)).isZero();
    }

    @Test
    @DisplayName("no index runner exists, the app never writes indexes")
    void noIndexRunner()
    {
        // Given the Mongo adapter type
        // When inspecting its declared methods
        // Then no geo-index backstop runner is present
        assertThat(Arrays.stream(MongoConfig.class.getMethods()).map(m -> m.getName())).doesNotContain(
                "ensureGeoIndex");
    }
}
