package com.barboleda.arbolado.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.barboleda.arbolado.domain.SearchRequest;
import com.barboleda.arbolado.web.ArbolController;
import com.mongodb.MongoException;

/**
 * ProblemDetail shape contract for every mapped failure (PLAN.md #6, #8).
 */
class GlobalExceptionHandlerTest
{

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("bean validation failure maps to 400")
    void validationIs400() throws Exception
    {
        // Given a bound field error like the one @Valid raises
        BindingResult binding = new BeanPropertyBindingResult(new SearchRequest(0.0, 0.0), "searchRequest");
        binding.addError(new FieldError("searchRequest", "latitude", 0.0, false, null, null,
                "must be less than or equal to 90"));
        Method search = BeanUtils.findMethod(ArbolController.class, "search", SearchRequest.class);
        MethodArgumentNotValidException failure = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(search, 0), binding);

        // When handling
        ResponseEntity<ProblemDetail> response = handler.handleValidation(failure);

        // Then the status is 400
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("malformed JSON maps to 400")
    void malformedIs400()
    {
        // Given an unreadable body
        HttpMessageNotReadableException failure = new HttpMessageNotReadableException("malformed",
                new MockHttpInputMessage(new byte[0]));

        // When handling
        // Then the status is 400
        assertThat(handler.handleNotReadable(failure).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("wrong media type maps to 415 and wrong method to 405")
    void mediaAndMethodMappings() throws Exception
    {
        // Given media and method mismatches
        // When handling
        // Then each maps to its own status
        assertThat(handler.handleUnsupportedMedia(new HttpMediaTypeNotSupportedException("text/plain"))
                .getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(handler.handleMethodNotAllowed(new HttpRequestMethodNotSupportedException("GET"))
                .getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    @DisplayName("finite-guard failure maps narrowly to 400")
    void guardIs400()
    {
        // Given the service-level guard exception
        // When handling
        // Then the status is 400 with the reason preserved
        ResponseEntity<ProblemDetail> response = handler
                .handleInvalidSearch(new InvalidSearchRequestException("Search latitude must be finite, got NaN"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getDetail()).contains("must be finite");
    }

    @Test
    @DisplayName("driver and Spring-wrapped data failures map to 503")
    void dataFailuresAre503()
    {
        // Given a raw driver error and its Spring-wrapped form
        // When handling
        // Then both surface as 503
        assertThat(handler.handleMongo(new MongoException("timeout")).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(handler.handleDataAccess(new DataAccessResourceFailureException("down")).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("unknown path maps to 404 carrying the resource path")
    void unknownPathIs404()
    {
        // Given an unmatched path
        NoResourceFoundException failure = new NoResourceFoundException(HttpMethod.GET, "/nope");

        // When handling
        ResponseEntity<ProblemDetail> response = handler.handleNotFound(failure);

        // Then the status is 404 with the path preserved
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getDetail()).contains("/nope");
    }

    @Test
    @DisplayName("fallback maps to a generic 500 with no leakage")
    void fallbackIsGeneric500()
    {
        // Given an unexpected bug carrying a sensitive message
        // When handling
        // Then the status is 500 and the detail hides the cause
        ResponseEntity<ProblemDetail> response = handler.handleFallback(new IllegalStateException("secret-conn"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getDetail()).doesNotContain("secret-conn");
    }
}
