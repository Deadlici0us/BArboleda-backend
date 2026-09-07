package com.barboleda.arbolado.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/**
 * Factory for RFC 9457 ProblemDetail responses.
 */
public class ProblemDetailFactory
{

    public ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail)
    {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
