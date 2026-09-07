package com.barboleda.arbolado.domain;

/**
 * Shared numeric limits for geospatial search.
 *
 * <p>Every bound exposed to Bean Validation annotations has a dual form: the numeric
 * constant used in code and a {@code String} constant used in the annotations, which
 * require compile-time {@code String} values. {@code SearchLimitsTest} asserts both
 * forms stay in sync.
 */
public final class SearchLimits
{

    /** Decimals kept when normalizing coordinates (~11m grid). */
    public static final int COORDINATE_SCALE = 4;

    /** Minimum latitude in degrees. */
    public static final double MIN_LATITUDE = -90.0;

    /** Minimum latitude as an annotation value. */
    public static final String MIN_LATITUDE_STR = "-90";

    /** Maximum latitude in degrees. */
    public static final double MAX_LATITUDE = 90.0;

    /** Maximum latitude as an annotation value. */
    public static final String MAX_LATITUDE_STR = "90";

    /** Minimum longitude in degrees. */
    public static final double MIN_LONGITUDE = -180.0;

    /** Minimum longitude as an annotation value. */
    public static final String MIN_LONGITUDE_STR = "-180";

    /** Maximum longitude in degrees. */
    public static final double MAX_LONGITUDE = 180.0;

    /** Maximum longitude as an annotation value. */
    public static final String MAX_LONGITUDE_STR = "180";

    /** Minimum search radius in whole meters. */
    public static final int MIN_RADIUS_METERS = 1;

    /** Minimum search radius as an annotation value. */
    public static final String MIN_RADIUS_STR = "1";

    /** Maximum search radius in whole meters. */
    public static final int MAX_RADIUS_METERS = 1000;

    /** Maximum search radius as an annotation value. */
    public static final String MAX_RADIUS_STR = "1000";

    /** Maximum items returned per search before truncation. */
    public static final int MAX_ITEMS = 100;

    /** Spring cache name for search results. */
    public static final String CACHE_NAME = "arboles";

    private SearchLimits()
    {
    }
}
