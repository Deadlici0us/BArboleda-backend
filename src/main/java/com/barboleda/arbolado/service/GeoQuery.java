package com.barboleda.arbolado.service;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;

/**
 * Center point plus maximum distance for one geospatial search.
 *
 * @param center query center in lon-first order
 * @param maxDistance maximum distance in kilometers for the {@code 2dsphere} query
 */
public record GeoQuery(Point center, Distance maxDistance)
{
}
