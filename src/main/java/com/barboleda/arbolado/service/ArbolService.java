package com.barboleda.arbolado.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.barboleda.arbolado.domain.ArbolResponse;
import com.barboleda.arbolado.domain.SearchLimits;
import com.barboleda.arbolado.domain.SearchRequest;
import com.barboleda.arbolado.domain.SearchResponse;

/**
 * Search facade: guards, normalizes, delegates to the fixed 1000m cached path,
 * slices and wraps.
 */
@Component
public class ArbolService implements ArbolSearchFacade
{

    private final CoordinateNormalizationStrategy normalizer;

    private final CachedArbolSearch cachedSearch;

    private final SearchInputValidator validator;

    private final SearchResultWindow window;

    /**
     * Builds the facade over normalization and cached search.
     *
     * @param normalizer the coordinate grid strategy
     * @param cachedSearch the cache-owning search bean
     * @param validator the input validation guard
     * @param window the result slicer
     */
    public ArbolService(CoordinateNormalizationStrategy normalizer, CachedArbolSearch cachedSearch,
            SearchInputValidator validator, SearchResultWindow window)
    {
        this.normalizer = normalizer;
        this.cachedSearch = cachedSearch;
        this.validator = validator;
        this.window = window;
    }

    /**
     * Runs one geospatial search end to end. Fixed 1000m bucket.
     *
     * @param request the validated request
     * @return the sliced, wrapped response with normalized inputs echoed back
     * @throws com.barboleda.arbolado.exception.InvalidSearchRequestException
     *         on non-finite latitude or longitude
     */
    @Override
    public SearchResponse findNearby(SearchRequest request)
    {
        validator.requireFinite(request.latitude(), "latitude");
        validator.requireFinite(request.longitude(), "longitude");

        double latitude = normalizer.normalize(request.latitude());
        double longitude = normalizer.normalize(request.longitude());

        List<ArbolResponse> fetched = cachedSearch.findNearbyCached(
                latitude, longitude, new RadiusMeters(SearchLimits.FIXED_RADIUS_METERS));
        SearchResultWindow.WindowResult result = window.window(fetched);
        return new SearchResponse(
                result.items(), result.items().size(), result.truncated(),
                latitude, longitude, SearchLimits.FIXED_RADIUS_METERS);
    }
}
