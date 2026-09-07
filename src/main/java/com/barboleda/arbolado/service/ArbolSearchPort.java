package com.barboleda.arbolado.service;

import java.util.List;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;

import com.barboleda.arbolado.domain.Arbol;

/**
 * DIP port for geospatial tree search; the service depends on this, never on Mongo types.
 */
public interface ArbolSearchPort
{

    /**
     * Finds trees near a center point, raw and unsliced.
     *
     * @param center query center in lon-first order
     * @param maxDistance maximum distance in kilometers
     * @return matching trees in ascending distance order, up to the caller's probe size
     */
    List<Arbol> searchNear(Point center, Distance maxDistance);
}
