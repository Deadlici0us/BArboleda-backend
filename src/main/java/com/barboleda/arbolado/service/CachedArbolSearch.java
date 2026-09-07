package com.barboleda.arbolado.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.barboleda.arbolado.domain.ArbolResponse;
import com.barboleda.arbolado.domain.SearchLimits;

/**
 * Cache-owning search step: port call plus DTO mapping behind one cache entry.
 *
 * <p>A separate bean (never a self-call) so the Spring cache proxy actually applies.
 * {@code sync} coalesces concurrent misses only on locking caches; on Redis every
 * cold miss runs the loader (pinned by RedisCacheContractIT). Returns up to
 * {@code MAX_ITEMS + 1} DTOs unsliced; the service slices and sets the flag.
 */
@Component
public class CachedArbolSearch
{

    private final ArbolSearchPort searchPort;

    private final ArbolMapper mapper;

    /**
     * Builds the cached search over its collaborators.
     *
     * @param searchPort the geo search port
     * @param mapper the entity-to-DTO mapper
     */
    public CachedArbolSearch(ArbolSearchPort searchPort, ArbolMapper mapper)
    {
        this.searchPort = searchPort;
        this.mapper = mapper;
    }

    /**
     * Searches through the {@code arboles} cache entry for one normalized input.
     *
     * @param latitude normalized latitude
     * @param longitude normalized longitude
     * @param radiusMeters already-rounded radius in whole meters
     * @return mapped DTOs in ascending distance order, up to {@code MAX_ITEMS + 1}
     */
    @Cacheable(value = SearchLimits.CACHE_NAME, cacheResolver = "failOpenCacheResolver",
            keyGenerator = "cacheKeyGenerator", sync = true)
    public List<ArbolResponse> findNearbyCached(double latitude, double longitude, int radiusMeters)
    {
        GeoCenter center = new GeoCenter(longitude, latitude);
        RadiusMeters radius = new RadiusMeters(radiusMeters);
        return searchPort.searchNear(center, radius).stream().map(mapper::toResponse).toList();
    }
}
