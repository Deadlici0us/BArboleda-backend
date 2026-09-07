package com.barboleda.arbolado.service;

import com.barboleda.arbolado.exception.InvalidSearchRequestException;

/**
 * Service-level guard: validates search inputs are finite before normalization.
 */
public class SearchInputValidator
{

    /**
     * Validates one input value is finite and non-null.
     *
     * @param value the value to validate
     * @param name the input name for the error message
     * @throws InvalidSearchRequestException if null, NaN, or infinite
     */
    public void requireFinite(Double value, String name)
    {
        if (value == null || !Double.isFinite(value))
        {
            throw new InvalidSearchRequestException(
                    "Search " + name + " must be finite, got " + value);
        }
    }
}
