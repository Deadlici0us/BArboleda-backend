package com.barboleda.arbolado.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Bean Validation contract for {@link SearchRequest} via a programmatic validator.
 *
 * <p>NaN/Infinity are covered by the service-level finite guard, not here: Bean
 * Validation min/max paths treat NaN inconsistently (PLAN.md #3).
 */
class SearchRequestValidationTest
{

    private static ValidatorFactory validatorFactory;

    private static Validator validator;

    /**
     * Opens the validator factory once for the whole test class.
     */
    @BeforeAll
    static void openValidator()
    {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    /**
     * Closes the validator factory after all tests.
     */
    @AfterAll
    static void closeValidator()
    {
        validatorFactory.close();
    }

    @Test
    @DisplayName("typical Buenos Aires request passes validation")
    void validRequestPasses()
    {
        // Given a well-formed request
        SearchRequest request = new SearchRequest(-34.6037, -58.3816, 500.0);

        // When validating
        Set<ConstraintViolation<SearchRequest>> violations = validator.validate(request);

        // Then there are none
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("null coordinates and radius are rejected")
    void nullFieldsRejected()
    {
        // Given requests with each field nulled
        // When validating
        // Then every one reports a violation
        assertThat(validator.validate(new SearchRequest(null, -58.3816, 500.0))).isNotEmpty();
        assertThat(validator.validate(new SearchRequest(-34.6037, null, 500.0))).isNotEmpty();
        assertThat(validator.validate(new SearchRequest(-34.6037, -58.3816, null))).isEmpty(); // optional
    }

    @Test
    @DisplayName("zero and negative radius are rejected by the 1m floor")
    void zeroAndNegativeRadiusRejected()
    {
        // Given requests below the 1m floor
        // When validating
        // Then both are rejected
        assertThat(validator.validate(new SearchRequest(-34.6037, -58.3816, 0.0))).isNotEmpty();
        assertThat(validator.validate(new SearchRequest(-34.6037, -58.3816, -5.0))).isNotEmpty();
    }

    @Test
    @DisplayName("radius 1000 passes but 1000.01 fails")
    void radiusUpperEdge()
    {
        // Given requests at and just above the 1000m ceiling
        // When validating
        // Then the ceiling value passes and anything above fails
        assertThat(validator.validate(new SearchRequest(-34.6037, -58.3816, 1000.0))).isEmpty();
        assertThat(validator.validate(new SearchRequest(-34.6037, -58.3816, 1000.01))).isNotEmpty();
    }

    @Test
    @DisplayName("fractional radius below 1 or above 1000 is rejected before rounding")
    void fractionalRadiusBoundaries()
    {
        // Given fractional radii just outside the bounds
        // When validating
        // Then both are rejected instead of rounding into range
        assertThat(validator.validate(new SearchRequest(-34.6037, -58.3816, 0.5))).isNotEmpty();
        assertThat(validator.validate(new SearchRequest(-34.6037, -58.3816, 1000.5))).isNotEmpty();
    }

    @Test
    @DisplayName("fractional radius passes validation; rounding is the service's job")
    void fractionalRadiusPassesValidation()
    {
        // Given a fractional radius inside the bounds
        SearchRequest request = new SearchRequest(-34.6037, -58.3816, 499.6);

        // When validating
        Set<ConstraintViolation<SearchRequest>> violations = validator.validate(request);

        // Then it passes — ArbolService rounds to whole meters afterwards
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("latitude edges ±90 pass, anything beyond fails")
    void latitudeEdges()
    {
        // Given requests on and beyond the latitude bounds
        // When validating
        // Then the edges pass and out-of-range values fail
        assertThat(validator.validate(new SearchRequest(90.0, -58.3816, 500.0))).isEmpty();
        assertThat(validator.validate(new SearchRequest(-90.0, -58.3816, 500.0))).isEmpty();
        assertThat(validator.validate(new SearchRequest(90.0001, -58.3816, 500.0))).isNotEmpty();
        assertThat(validator.validate(new SearchRequest(-90.0001, -58.3816, 500.0))).isNotEmpty();
    }

    @Test
    @DisplayName("longitude edges ±180 pass, anything beyond fails")
    void longitudeEdges()
    {
        // Given requests on and beyond the longitude bounds
        // When validating
        // Then the edges pass and out-of-range values fail
        assertThat(validator.validate(new SearchRequest(-34.6037, 180.0, 500.0))).isEmpty();
        assertThat(validator.validate(new SearchRequest(-34.6037, -180.0, 500.0))).isEmpty();
        assertThat(validator.validate(new SearchRequest(-34.6037, 180.0001, 500.0))).isNotEmpty();
        assertThat(validator.validate(new SearchRequest(-34.6037, -180.0001, 500.0))).isNotEmpty();
    }
}
