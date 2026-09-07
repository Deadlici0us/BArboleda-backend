package com.barboleda.arbolado.exception;

/**
 * Rejected search input from the service-level finite guard.
 *
 * <p>Extends {@code IllegalArgumentException} but stays narrowly mapped to 400: the
 * handler must match this type exactly, never blanket {@code IllegalArgumentException},
 * which would mask 500s from library bugs.
 */
public class InvalidSearchRequestException extends IllegalArgumentException
{

    /**
     * Builds the exception with a detail message.
     *
     * @param message what was invalid and why
     */
    public InvalidSearchRequestException(String message)
    {
        super(message);
    }
}
