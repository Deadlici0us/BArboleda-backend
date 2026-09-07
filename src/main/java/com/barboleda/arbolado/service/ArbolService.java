package com.barboleda.arbolado.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.barboleda.arbolado.domain.ArbolResponse;
import com.barboleda.arbolado.domain.SearchLimits;
import com.barboleda.arbolado.domain.SearchRequest;
import com.barboleda.arbolado.domain.SearchResponse;
import com.barboleda.arbolado.exception.InvalidSearchRequestException;

/**
 * Search facade: guards, normalizes, delegates to the cached path, then slices and wraps.
 */
@Component
public class ArbolService
{

    private final CoordinateNormalizationStrategy normalizer;

    private final CachedArbolSearch cachedSearch;

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
    }

    /**
     * Runs one geospatial search end to end.
     *
     * @param request the validated request; fractional radii round to whole meters first
     * @return the sliced, wrapped response with normalized inputs echoed back
     * @throws InvalidSearchRequestException on non-finite latitude, longitude or radius
     */
    public SearchResponse findNearby(SearchRequest request)
    {
        requireFinite(request.latitude(), "latitude");
        requireFinite(request.longitude(), "longitude");
        requireFinite(request.radius(), "radius");
        double latitude = normalizer.normalize(request.latitude());
        double longitude = normalizer.normalize(request.longitude());
        int radiusMeters = (int) Math.round(request.radius());
        List<ArbolResponse> fetched = cachedSearch.findNearbyCached(latitude, longitude, radiusMeters);
        boolean truncated = fetched.size() > SearchLimits.MAX_ITEMS;
        List<ArbolResponse> items = fetched.stream().limit(SearchLimits.MAX_ITEMS).toList();
        return new SearchResponse(items, items.size(), truncated, latitude, longitude, radiusMeters);
    }

    private void requireFinite(Double value, String name)
    {
        if (value == null || !Double.isFinite(value))
        {
            throw new InvalidSearchRequestException("Search " + name + " must be finite, got " + value);
        }
    }
}
