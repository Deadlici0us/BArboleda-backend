package com.barboleda.arbolado.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Correlation filter contract (PLAN.md #0, #8).
 */
class CorrelationIdFilterTest
{

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    @DisplayName("incoming request id propagates to response and MDC, then clears")
    void propagatesAndClears() throws Exception
    {
        // Given a request carrying an id
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "req-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenInChain = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain()
        {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest servletRequest,
                    jakarta.servlet.ServletResponse servletResponse)
            {
                seenInChain.set(MDC.get(CorrelationIdFilter.MDC_KEY));
            }
        };

        // When filtering
        filter.doFilter(request, response, chain);

        // Then the id echoes on the response, was visible downstream, and MDC cleared after
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("req-1");
        assertThat(seenInChain.get()).isEqualTo("req-1");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("missing request id generates a UUID")
    void generatesWhenAbsent() throws Exception
    {
        // Given a request without an id
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When filtering
        filter.doFilter(request, response, new MockFilterChain());

        // Then a valid UUID echoes on the response
        assertThat(UUID.fromString(response.getHeader(CorrelationIdFilter.HEADER))).isNotNull();
    }

    @Test
    @DisplayName("filter runs first in the chain")
    void runsFirst()
    {
        // Given the filter registration
        // When inspecting
        // Then it carries the highest precedence order
        Order order = CorrelationIdFilter.class.getAnnotation(Order.class);
        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
