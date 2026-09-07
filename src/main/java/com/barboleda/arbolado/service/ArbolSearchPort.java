package com.barboleda.arbolado.service;

import java.util.List;

import com.barboleda.arbolado.domain.Arbol;

/**
 * DIP port for geospatial tree search; the service depends on this, never on Mongo types.
 */
public interface ArbolSearchPort
{

    /**
     * Finds trees near a center point, raw and unsliced.
     *
     * @param center normalized search center in lon-first order
     * @param radius rounded radius in whole meters
     * @return matching trees in ascending distance order, up to the caller's probe size
     */
    List<Arbol> searchNear(GeoCenter center, RadiusMeters radius);
}
