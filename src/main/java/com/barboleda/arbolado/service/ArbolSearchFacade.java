package com.barboleda.arbolado.service;

import com.barboleda.arbolado.domain.SearchRequest;
import com.barboleda.arbolado.domain.SearchResponse;

/**
 * Facade interface for the search domain (DIP for controllers/test mocks).
 */
public interface ArbolSearchFacade
{

    /**
     * Runs one geospatial search end to end.
     *
     * @param request validated search input
     * @return wrapped, distance-sorted response
     */
    SearchResponse findNearby(SearchRequest request);
}
