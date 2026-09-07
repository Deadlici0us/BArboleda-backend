package com.barboleda.arbolado.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the numeric/String constant pairs in {@link SearchLimits} against drift.
 */
class SearchLimitsTest
{

    @Test
    @DisplayName("coordinate scale stays at 4 decimals (~11m grid)")
    void coordinateScaleIsFour()
    {
        // Given the locked normalization precision from PLAN.md #0
        // When reading the scale constant
        // Then it is 4
        assertThat(SearchLimits.COORDINATE_SCALE).isEqualTo(4);
    }

    @Test
    @DisplayName("latitude/longitude numeric and String bounds stay in sync")
    void coordinateBoundsStayInSync()
    {
        // Given the dual numeric + String forms required by Bean Validation annotations
        // When parsing the String forms
        // Then they equal the numeric forms
        assertThat(Double.parseDouble(SearchLimits.MIN_LATITUDE_STR)).isEqualTo(SearchLimits.MIN_LATITUDE);
        assertThat(Double.parseDouble(SearchLimits.MAX_LATITUDE_STR)).isEqualTo(SearchLimits.MAX_LATITUDE);
        assertThat(Double.parseDouble(SearchLimits.MIN_LONGITUDE_STR)).isEqualTo(SearchLimits.MIN_LONGITUDE);
        assertThat(Double.parseDouble(SearchLimits.MAX_LONGITUDE_STR)).isEqualTo(SearchLimits.MAX_LONGITUDE);
    }

    @Test
    @DisplayName("radius numeric and String bounds stay in sync")
    void radiusBoundsStayInSync()
    {
        // Given the dual int + String forms required by Bean Validation annotations
        // When parsing the String forms
        // Then they equal the numeric forms
        assertThat(Integer.parseInt(SearchLimits.MIN_RADIUS_STR)).isEqualTo(SearchLimits.MIN_RADIUS_METERS);
        assertThat(Integer.parseInt(SearchLimits.MAX_RADIUS_STR)).isEqualTo(SearchLimits.MAX_RADIUS_METERS);
    }

    @Test
    @DisplayName("result cap and cache name stay locked")
    void capsStayLocked()
    {
        // Given the locked service limits from PLAN.md #0
        // When reading them
        // Then they match the contract
        assertThat(SearchLimits.MAX_ITEMS).isEqualTo(100);
        assertThat(SearchLimits.CACHE_NAME).isEqualTo("arboles");
    }
}
