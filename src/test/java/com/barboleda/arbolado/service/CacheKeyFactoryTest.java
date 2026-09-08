package com.barboleda.arbolado.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Key stability contract for {@link CacheKeyFactory} (3-decimal grid, fixed-radius bucket).
 */
class CacheKeyFactoryTest
{

    private final CacheKeyFactory factory = new CacheKeyFactory();

    @Test
    @DisplayName("same inputs always produce the same key")
    void determinism()
    {
        assertThat(factory.create(-34.6037, -58.3816))
                .isEqualTo(factory.create(-34.6037, -58.3816));
    }

    @Test
    @DisplayName("key format is 3-decimal pinned exactly, no radius segment")
    void fixedPrecisionFormat()
    {
        assertThat(factory.create(4.5, -58.3816))
                .isEqualTo(factory.create(4.500, -58.3816));
        assertThat(factory.create(-34.6037, -58.3816))
                .isEqualTo("-34.604:-58.382");
    }

    @Test
    @DisplayName("signed zero canonicalizes so -0.0 shares the 0.0 key")
    void negativeZeroCanonicalized()
    {
        String key = factory.create(-0.0, -0.0);
        assertThat(key).isEqualTo(factory.create(0.0, 0.0));
        assertThat(key).doesNotContain("-0.000");
    }

    @Test
    @DisplayName("NaN and infinite coordinates are rejected")
    void nonFiniteRejected()
    {
        assertThatIllegalArgumentException().isThrownBy(() -> factory.create(Double.NaN, -58.3816));
        assertThatIllegalArgumentException().isThrownBy(() -> factory.create(-34.6037, Double.NaN));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> factory.create(Double.POSITIVE_INFINITY, -58.3816));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> factory.create(-34.6037, Double.NEGATIVE_INFINITY));
    }
}
