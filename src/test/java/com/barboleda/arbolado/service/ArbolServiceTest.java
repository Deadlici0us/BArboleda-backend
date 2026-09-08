package com.barboleda.arbolado.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.barboleda.arbolado.domain.ArbolResponse;
import com.barboleda.arbolado.domain.SearchLimits;
import com.barboleda.arbolado.domain.SearchRequest;
import com.barboleda.arbolado.domain.SearchResponse;
import com.barboleda.arbolado.exception.InvalidSearchRequestException;

/**
 * Facade contract with mocked cached search (fixed 1000m bucket).
 */
class ArbolServiceTest
{

    private final CachedArbolSearch cachedSearch = mock(CachedArbolSearch.class);

    private final ArbolService service = new ArbolService(new RoundingNormalizationStrategy(), cachedSearch,
            new SearchInputValidator(), new SearchResultWindow());

    @Test
    @DisplayName("non-finite latitude or longitude throws before touching cache")
    void finiteGuardRejects()
    {
        assertThatThrownBy(() -> service.findNearby(new SearchRequest(Double.NaN, -58.3816)))
                .isInstanceOf(InvalidSearchRequestException.class);
        assertThatThrownBy(() -> service.findNearby(new SearchRequest(-34.6037, Double.NaN)))
                .isInstanceOf(InvalidSearchRequestException.class);
        assertThatThrownBy(
                () -> service.findNearby(new SearchRequest(Double.POSITIVE_INFINITY, -58.3816)))
                .isInstanceOf(InvalidSearchRequestException.class);
        assertThatThrownBy(
                () -> service.findNearby(new SearchRequest(-34.6037, Double.NEGATIVE_INFINITY)))
                .isInstanceOf(InvalidSearchRequestException.class);

        // Non-finite inputs must never touch the cached search
        org.mockito.Mockito.verifyNoInteractions(cachedSearch);
    }

    @Test
    @DisplayName("backend always passes fixed 1000m bucket to adapter")
    void fixedBucket()
    {
        when(cachedSearch.findNearbyCached(anyDouble(), anyDouble(), any())).thenReturn(List.of());
        service.findNearby(new SearchRequest(-34.6037, -58.3816));
        service.findNearby(new SearchRequest(-34.6037, -58.3816));
        // All calls receive the fixed 1000m bucket
        verify(cachedSearch, times(2)).findNearbyCached(
                anyDouble(), anyDouble(), org.mockito.ArgumentMatchers.argThat(
                        r -> ((RadiusMeters) r).value() == SearchLimits.FIXED_RADIUS_METERS));
    }

    @Test
    @DisplayName("truncated is true when more than MAX_ITEMS (1000) matched")
    void truncatedOnlyOnOverflow()
    {
        when(cachedSearch.findNearbyCached(anyDouble(), anyDouble(), any())).thenReturn(probeDtos(1001));
        SearchResponse overflow = service.findNearby(new SearchRequest(-34.6037, -58.3816));
        assertThat(overflow.items()).hasSize(SearchLimits.MAX_ITEMS);
        assertThat(overflow.total()).isEqualTo(SearchLimits.MAX_ITEMS);
        assertThat(overflow.truncated()).isTrue();
        assertThat(overflow.radiusMeters()).isEqualTo(SearchLimits.FIXED_RADIUS_METERS);

        when(cachedSearch.findNearbyCached(anyDouble(), anyDouble(), any())).thenReturn(probeDtos(1000));
        SearchResponse exact = service.findNearby(new SearchRequest(-34.6037, -58.3816));
        assertThat(exact.items()).hasSize(SearchLimits.MAX_ITEMS);
        assertThat(exact.total()).isEqualTo(SearchLimits.MAX_ITEMS);
        assertThat(exact.truncated()).isFalse();
        assertThat(exact.radiusMeters()).isEqualTo(SearchLimits.FIXED_RADIUS_METERS);
    }

    @Test
    @DisplayName("empty result wraps to 200 with fixed 1000m bucket")
    void emptyResultWraps()
    {
        when(cachedSearch.findNearbyCached(anyDouble(), anyDouble(), any())).thenReturn(List.of());
        SearchResponse response = service.findNearby(new SearchRequest(-34.6037, -58.3816));
        assertThat(response.items()).isEmpty();
        assertThat(response.total()).isZero();
        assertThat(response.truncated()).isFalse();
        assertThat(response.normalizedLatitude()).isEqualTo(-34.604); // 3-decimal snap
        assertThat(response.normalizedLongitude()).isEqualTo(-58.382);
        assertThat(response.radiusMeters()).isEqualTo(SearchLimits.FIXED_RADIUS_METERS);
    }

    private List<ArbolResponse> probeDtos(int count)
    {
        return IntStream.range(0, count)
                .mapToObj(i -> new ArbolResponse(i, "E", 1, 1, 1, 0.0, 0.0))
                .toList();
    }
}
