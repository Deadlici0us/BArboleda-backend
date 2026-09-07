package com.barboleda.arbolado.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Key stability contract for {@link CacheKeyFactory} (PLAN.md #5, #8).
 */
class CacheKeyFactoryTest
{

    private final CacheKeyFactory factory = new CacheKeyFactory();

    @Test
    @DisplayName("same inputs always produce the same key")
    void determinism()
    {
        // Given one normalized input triple
        // When creating the key twice
        // Then both keys are identical
        assertThat(factory.create(-34.6037, -58.3816, 500))
                .isEqualTo(factory.create(-34.6037, -58.3816, 500));
    }

    @Test
    @DisplayName("key format is fixed-precision and pinned exactly")
    void fixedPrecisionFormat()
    {
        // Given inputs that differ only past the 4th decimal
        // When creating keys
        // Then they collapse onto one pinned key
        assertThat(factory.create(4.5, -58.3816, 500))
                .isEqualTo(factory.create(4.5000, -58.3816, 500));
        assertThat(factory.create(-34.6037, -58.3816, 500))
                .isEqualTo("-34.6037:-58.3816:500");
    }

    @Test
    @DisplayName("signed zero canonicalizes so -0.0 shares the 0.0 key")
    void negativeZeroCanonicalized()
    {
        // Given signed-zero coordinates
        // When creating keys
        // Then they equal the positive-zero key with no "-0.0000" rendering
        String key = factory.create(-0.0, -0.0, 500);
        assertThat(key).isEqualTo(factory.create(0.0, 0.0, 500));
        assertThat(key).doesNotContain("-0.0000");
    }

    @Test
    @DisplayName("null, NaN and infinite coordinates are rejected")
    void nonFiniteRejected()
    {
        // Given missing, NaN or infinite coordinates
        // When creating a key
        // Then each is rejected fast instead of poisoning the cache namespace
        assertThatIllegalArgumentException().isThrownBy(() -> factory.create(null, -58.3816, 500));
        assertThatIllegalArgumentException().isThrownBy(() -> factory.create(-34.6037, null, 500));
        assertThatIllegalArgumentException().isThrownBy(() -> factory.create(Double.NaN, -58.3816, 500));
        assertThatIllegalArgumentException().isThrownBy(() -> factory.create(-34.6037, Double.NaN, 500));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> factory.create(Double.POSITIVE_INFINITY, -58.3816, 500));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> factory.create(-34.6037, Double.NEGATIVE_INFINITY, 500));
    }
}
