package com.barboleda.arbolado.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.barboleda.arbolado.domain.ArbolResponse;
import com.barboleda.arbolado.domain.SearchRequest;
import com.barboleda.arbolado.domain.SearchResponse;

/**
 * Search facade: guards, normalizes, delegates to the cached path, then slices and wraps.
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
     */
    public ArbolService(CoordinateNormalizationStrategy normalizer, CachedArbolSearch cachedSearch)
    {
        this.normalizer = normalizer;
        this.cachedSearch = cachedSearch;
        this.validator = new SearchInputValidator();
        this.window = new SearchResultWindow();
    }

    /**
     * Runs one geospatial search end to end.
     *
     * @param request the validated request; fractional radii round to whole meters first
     * @return the sliced, wrapped response with normalized inputs echoed back
     * @throws com.barboleda.arbolado.exception.InvalidSearchRequestException
     *         on non-finite latitude, longitude or radius
     */
    @Override
    public SearchResponse findNearby(SearchRequest request)
    {
        validator.requireFinite(request.latitude(), "latitude");
        validator.requireFinite(request.longitude(), "longitude");
        validator.requireFinite(request.radius(), "radius");

        double latitude = normalizer.normalize(request.latitude());
        double longitude = normalizer.normalize(request.longitude());
        int radiusMeters = (int) Math.round(request.radius());
        new RadiusMeters(radiusMeters); // validates 1..1000

        List<ArbolResponse> fetched = cachedSearch.findNearbyCached(latitude, longitude, radiusMeters);
        SearchResultWindow.WindowResult result = window.window(fetched);
        return new SearchResponse(result.items(), result.items().size(), result.truncated(),
                latitude, longitude, radiusMeters);
    }
}
