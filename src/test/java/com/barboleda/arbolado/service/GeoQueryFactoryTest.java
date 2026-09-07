package com.barboleda.arbolado.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Metrics;

/**
 * Geo query value contract (PLAN.md #5, #8).
 */
class GeoQueryFactoryTest
{

    private final GeoQueryFactory factory = new GeoQueryFactory();

    @Test
    @DisplayName("point keeps lon-first order")
    void lonFirstOrder()
    {
        // Given a lon/lat pair where a flip is visible
        // When creating the query
        // Then x is longitude and y is latitude
        GeoQuery query = factory.create(10.0, 20.0, 500);
        assertThat(query.center().getX()).isEqualTo(10.0);
        assertThat(query.center().getY()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("meters convert to kilometers with a floating-point divisor")
    void metersToKilometers()
    {
        // Given the 1m and 1000m edges
        // When creating queries
        // Then the KM distance matches radius/1000.0 exactly
        assertThat(factory.create(-58.3816, -34.6037, 1).maxDistance().getValue()).isEqualTo(0.001);
        assertThat(factory.create(-58.3816, -34.6037, 1000).maxDistance().getValue()).isEqualTo(1.0);
        assertThat(factory.create(-58.3816, -34.6037, 500).maxDistance().getMetric()).isEqualTo(Metrics.KILOMETERS);
    }

    @Test
    @DisplayName("null coordinates and out-of-range radii are rejected")
    void guards()
    {
        // Given invalid factory inputs
        // When creating queries
        // Then each is rejected before reaching Mongo
        assertThatIllegalArgumentException().isThrownBy(() -> factory.create(null, -34.6037, 500));
        assertThatIllegalArgumentException().isThrownBy(() -> factory.create(-58.3816, null, 500));
        assertThatIllegalArgumentException().isThrownBy(() -> factory.create(-58.3816, -34.6037, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> factory.create(-58.3816, -34.6037, 1001));
    }
}
