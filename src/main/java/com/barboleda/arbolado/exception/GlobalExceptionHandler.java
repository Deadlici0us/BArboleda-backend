package com.barboleda.arbolado.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.mongodb.MongoException;

/**
 * Maps failures onto RFC 9457 {@code ProblemDetail} responses with no stacktrace leakage.
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{

    /**
     * Handles bean validation failures on the request body.
     *
     * @param failure the validation failure
     * @return 400 with the first field error
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException failure)
    {
        String detail = failure.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage()).orElse("Invalid request");
        return problem(HttpStatus.BAD_REQUEST, detail);
    }

    /**
     * Handles unreadable bodies such as malformed JSON.
     *
     * @param failure the read failure
     * @return 400 without echoing the raw body
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException failure)
    {
        return problem(HttpStatus.BAD_REQUEST, "Malformed JSON request body");
    }

    /**
     * Handles non-JSON content types.
     *
     * @param failure the media type failure
     * @return 415
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedMedia(HttpMediaTypeNotSupportedException failure)
    {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type must be application/json");
    }

    /**
     * Handles wrong HTTP methods on known paths.
     *
     * @param failure the method failure
     * @return 405
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(HttpRequestMethodNotSupportedException failure)
    {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Method " + failure.getMethod() + " is not supported here");
    }

    /**
     * Handles service-level guard rejections. Narrow on purpose: never blanket
     * {@code IllegalArgumentException}, which would mask 500s from library bugs.
     *
     * @param failure the guard rejection
     * @return 400 with the reason preserved
     */
    @ExceptionHandler(InvalidSearchRequestException.class)
    public ResponseEntity<ProblemDetail> handleInvalidSearch(InvalidSearchRequestException failure)
    {
        return problem(HttpStatus.BAD_REQUEST, failure.getMessage());
    }

    /**
     * Handles raw driver failures.
     *
     * @param failure the driver failure
     * @return 503 with a generic detail
     */
    @ExceptionHandler(MongoException.class)
    public ResponseEntity<ProblemDetail> handleMongo(MongoException failure)
    {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Datastore temporarily unavailable");
    }

    /**
     * Handles Spring-wrapped data failures such as connection and timeout errors.
     *
     * @param failure the data access failure
     * @return 503 with a generic detail
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> handleDataAccess(DataAccessException failure)
    {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Datastore temporarily unavailable");
    }

    /**
     * Handles unmatched paths with the same RFC 9457 shape as every other mapping.
     *
     * @param failure the missing-resource failure
     * @return 404 carrying the resource path
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NoResourceFoundException failure)
    {
        return problem(HttpStatus.NOT_FOUND, "No resource at " + failure.getResourcePath());
    }

    /**
     * Catches everything else with a generic payload.
     *
     * @param failure the unexpected failure
     * @return 500 that hides the cause
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleFallback(Exception failure)
    {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail)
    {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
