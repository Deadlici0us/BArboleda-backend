package com.barboleda.arbolado.web;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Propagates {@code X-Request-Id} through responses and the logging MDC.
 *
 * <p>Runs first so every downstream log line and the response carry the id.
 * Generates a UUID when the caller sends none, and always clears the MDC.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter
{

    /** Request/response header carrying the correlation id. */
    public static final String HEADER = "X-Request-Id";

    /** MDC key carrying the correlation id for ECS logs. */
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank())
        {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId);
        try
        {
            response.setHeader(HEADER, correlationId);
            chain.doFilter(request, response);
        }
        finally
        {
            MDC.remove(MDC_KEY);
        }
    }
}
