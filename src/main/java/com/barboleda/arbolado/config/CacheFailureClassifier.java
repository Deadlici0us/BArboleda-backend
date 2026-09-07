package com.barboleda.arbolado.config;

import org.springframework.cache.Cache;

/**
 * Classifies a cache failure: loader failures propagate untouched;
 * infrastructure outages fall back to direct load.
 */
public class CacheFailureClassifier
{

    /**
     * Returns true if the exception is a loader (Mongo/data) failure.
     *
     * @param failure the exception to classify
     * @return true if loader failure
     */
    public boolean isLoaderFailure(RuntimeException failure)
    {
        return failure instanceof Cache.ValueRetrievalException;
    }

    /**
     * Returns true if the exception is an infrastructure outage.
     *
     * @param failure the exception to classify
     * @return true if infrastructure outage
     */
    public boolean isCacheOutage(RuntimeException failure)
    {
        return !isLoaderFailure(failure);
    }
}
