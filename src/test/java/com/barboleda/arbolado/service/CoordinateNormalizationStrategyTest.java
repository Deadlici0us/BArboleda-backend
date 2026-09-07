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
    @DisplayName("already-rounded coordinate passes through unchanged")
    void unchangedPassthrough()
    {
        // Given an already 4-decimal coordinate
        // When normalizing
        // Then it is returned as-is
        assertThat(strategy.normalize(-34.6037)).isEqualTo(-34.6037);
        assertThat(strategy.normalize(90.0)).isEqualTo(90.0);
        assertThat(strategy.normalize(-180.0)).isEqualTo(-180.0);
        assertThat(strategy.normalize(5.0)).isEqualTo(5.0);
    }

    @Test
    @DisplayName("fifth decimal rounds half up at scale 4")
    void roundsHalfUp()
    {
        // Given values straddling the 5th decimal
        // When normalizing
        // Then HALF_UP applies at scale 4
        assertThat(strategy.normalize(1.23455)).isEqualTo(1.2346);
        assertThat(strategy.normalize(1.23454)).isEqualTo(1.2345);
    }

    @Test
    @DisplayName("two nearby inputs collapse onto one grid cell")
    void twoStepsCollapse()
    {
        // Given two inputs inside the same ~11m grid cell
        // When normalizing both
        // Then they produce the same key-stable value
        assertThat(strategy.normalize(1.23455)).isEqualTo(strategy.normalize(1.234551));
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
