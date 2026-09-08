package com.barboleda.arbolado.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contract for the 4-decimal coordinate normalization (PLAN.md #5, #8).
 */
class CoordinateNormalizationStrategyTest
{

    private final CoordinateNormalizationStrategy strategy = new RoundingNormalizationStrategy();

    @Test
    @DisplayName("already-rounded 3-decimal passes through")
    void unchangedPassthrough()
    {
        assertThat(strategy.normalize(-34.604)).isEqualTo(-34.604);
        assertThat(strategy.normalize(90.0)).isEqualTo(90.0);
        assertThat(strategy.normalize(-180.0)).isEqualTo(-180.0);
        assertThat(strategy.normalize(5.0)).isEqualTo(5.0);
    }

    @Test
    @DisplayName("4th decimal rounds half up at scale 3")
    void roundsHalfUp()
    {
        assertThat(strategy.normalize(1.2345)).isEqualTo(1.235);
        assertThat(strategy.normalize(1.2344)).isEqualTo(1.234);
    }

    @Test
    @DisplayName("two nearby inputs collapse onto one 3-decimal grid cell (~111m)")
    void twoStepsCollapse()
    {
        assertThat(strategy.normalize(1.2345)).isEqualTo(strategy.normalize(1.23451));
    }

    @Test
    @DisplayName("negative zero canonicalizes to positive zero")
    void negativeZeroBecomesZero()
    {
        // Given signed zero
        // When normalizing
        // Then the raw bits equal positive zero (== alone cannot tell them apart)
        long bits = Double.doubleToRawLongBits(strategy.normalize(-0.0));
        assertThat(bits).isEqualTo(Double.doubleToRawLongBits(0.0));
    }

    @Test
    @DisplayName("null, NaN and infinities are rejected")
    void nonFiniteRejected()
    {
        // Given non-finite or missing inputs
        // When normalizing
        // Then each is rejected fast instead of poisoning keys or queries
        assertThatIllegalArgumentException().isThrownBy(() -> strategy.normalize(null));
        assertThatIllegalArgumentException().isThrownBy(() -> strategy.normalize(Double.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> strategy.normalize(Double.POSITIVE_INFINITY));
        assertThatIllegalArgumentException().isThrownBy(() -> strategy.normalize(Double.NEGATIVE_INFINITY));
    }
}
