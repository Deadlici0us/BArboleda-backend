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
 * Facade contract with a mocked cached search (PLAN.md #5, #8).
 */
class ArbolServiceTest
{

    private final CachedArbolSearch cachedSearch = mock(CachedArbolSearch.class);

    private final ArbolService service = new ArbolService(new RoundingNormalizationStrategy(), cachedSearch,
            new SearchInputValidator(), new SearchResultWindow());

    @Test
    @DisplayName("non-finite input on any field throws before touching the cache")
    void finiteGuardRejects()
    {
        // Given requests with NaN or infinite values on each field
        // When searching
        // Then the narrow guard exception fires and the cache is never reached
        assertThatThrownBy(() -> service.findNearby(new SearchRequest(Double.NaN, -58.3816, 500.0)))
                .isInstanceOf(InvalidSearchRequestException.class);
        assertThatThrownBy(() -> service.findNearby(new SearchRequest(-34.6037, Double.NaN, 500.0)))
                .isInstanceOf(InvalidSearchRequestException.class);
        assertThatThrownBy(() -> service.findNearby(new SearchRequest(-34.6037, -58.3816, Double.NaN)))
                .isInstanceOf(InvalidSearchRequestException.class);
        assertThatThrownBy(() -> service.findNearby(new SearchRequest(Double.POSITIVE_INFINITY, -58.3816, 500.0)))
                .isInstanceOf(InvalidSearchRequestException.class);
        assertThatThrownBy(() -> service.findNearby(new SearchRequest(-34.6037, Double.NEGATIVE_INFINITY, 500.0)))
                .isInstanceOf(InvalidSearchRequestException.class);
        assertThatThrownBy(() -> service.findNearby(new SearchRequest(-34.6037, -58.3816, Double.POSITIVE_INFINITY)))
                .isInstanceOf(InvalidSearchRequestException.class);
        org.mockito.Mockito.verifyNoInteractions(cachedSearch);
    }

    @Test
    @DisplayName("fractional radii converge and diverge on the rounded whole meters")
    void radiusRoundedBeforeDelegating()
    {
        // Given fractional radii around the .5 boundary
        // When searching
        // Then the cached path always receives the rounded int, never the raw double
        when(cachedSearch.findNearbyCached(anyDouble(), anyDouble(), any())).thenReturn(List.of());
        service.findNearby(new SearchRequest(-34.6037, -58.3816, 499.6));
        service.findNearby(new SearchRequest(-34.6037, -58.3816, 500.4));
        service.findNearby(new SearchRequest(-34.6037, -58.3816, 499.4));
        verify(cachedSearch, times(2)).findNearbyCached(-34.6037, -58.3816, new RadiusMeters(500));
        verify(cachedSearch).findNearbyCached(-34.6037, -58.3816, new RadiusMeters(499));
    }

    @Test
    @DisplayName("rounded radius always lands inside the 1 to 1000 meter bounds")
    void roundedRadiusStaysInBounds()
    {
        // Given fractional radii hugging both bounds
        // When searching
        // Then the cached path receives the rounded int, never out of bounds
        when(cachedSearch.findNearbyCached(anyDouble(), anyDouble(), any())).thenReturn(List.of());
        service.findNearby(new SearchRequest(-34.6037, -58.3816, 1.4));
        service.findNearby(new SearchRequest(-34.6037, -58.3816, 999.6));
        verify(cachedSearch).findNearbyCached(-34.6037, -58.3816, new RadiusMeters(1));
        verify(cachedSearch).findNearbyCached(-34.6037, -58.3816, new RadiusMeters(1000));
    }

    @Test
    @DisplayName("truncated is true only when more than MAX_ITEMS matched")
    void truncatedOnlyOnOverflow()
    {
        // Given a full probe page versus an exact-cap page
        when(cachedSearch.findNearbyCached(anyDouble(), anyDouble(), any())).thenReturn(probeDtos(101));

        // When searching with overflow
        SearchResponse overflow = service.findNearby(new SearchRequest(-34.6037, -58.3816, 1000.0));

        // Then items slice to the cap with total capped and truncated set
        assertThat(overflow.items()).hasSize(SearchLimits.MAX_ITEMS);
        assertThat(overflow.total()).isEqualTo(SearchLimits.MAX_ITEMS);
        assertThat(overflow.truncated()).isTrue();

        // When searching with exactly MAX_ITEMS matches
        when(cachedSearch.findNearbyCached(anyDouble(), anyDouble(), any())).thenReturn(probeDtos(100));
        SearchResponse exact = service.findNearby(new SearchRequest(-34.6037, -58.3816, 1000.0));

        // Then nothing is truncated
        assertThat(exact.items()).hasSize(SearchLimits.MAX_ITEMS);
        assertThat(exact.total()).isEqualTo(SearchLimits.MAX_ITEMS);
        assertThat(exact.truncated()).isFalse();
    }

    @Test
    @DisplayName("empty result wraps to a 200 payload, never 404")
    void emptyResultWraps()
    {
        // Given no matches
        when(cachedSearch.findNearbyCached(anyDouble(), anyDouble(), any())).thenReturn(List.of());

        // When searching
        SearchResponse response = service.findNearby(new SearchRequest(-34.6037, -58.3816, 500.0));

        // Then the wrapper carries the empty contract plus normalized inputs
        assertThat(response.items()).isEmpty();
        assertThat(response.total()).isZero();
        assertThat(response.truncated()).isFalse();
        assertThat(response.normalizedLatitude()).isEqualTo(-34.6037);
        assertThat(response.normalizedLongitude()).isEqualTo(-58.3816);
        assertThat(response.radiusMeters()).isEqualTo(500);
    }

    private List<ArbolResponse> probeDtos(int count)
    {
        return IntStream.range(0, count)
                .mapToObj(i -> new ArbolResponse(i, "E", 1, 1, 1, 0.0, 0.0))
                .toList();
    }
}
