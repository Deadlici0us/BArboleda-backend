package com.barboleda.arbolado.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.barboleda.arbolado.domain.ArbolResponse;
import com.barboleda.arbolado.domain.SearchRequest;
import com.barboleda.arbolado.domain.SearchResponse;
import com.barboleda.arbolado.service.ArbolService;

/**
 * HTTP contract for {@code POST /search} (PLAN.md #6, #8).
 */
@WebMvcTest(ArbolController.class)
class ArbolControllerTest
{

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArbolService service;

    @Test
    @DisplayName("happy path returns 200 with the distance-sorted wrapper")
    void happyPath() throws Exception
    {
        // Given a service result (fixed 1000m bucket, 3-decimal snap)
        ArbolResponse dto = new ArbolResponse(123, "Jacaranda mimosifolia", 8, 30, 1, -58.3816, -34.6037);
        when(service.findNearby(any(SearchRequest.class)))
                .thenReturn(new SearchResponse(List.of(dto), 1, false, -34.604, -58.382, 1000));

        // When posting a valid search
        // Then the wrapper shape returns with the correlation header echoed
        mockMvc.perform(post("/search").contentType(MediaType.APPLICATION_JSON)
                .header("X-Request-Id", "req-1")
                .content("{\"latitude\":-34.6037,\"longitude\":-58.3816}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-1"))
                .andExpect(jsonPath("$.items[0].nro_registro").value(123))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.truncated").value(false))
                .andExpect(jsonPath("$.radiusMeters").value(1000));
    }

    @Test
    @DisplayName("out-of-range, missing and malformed bodies return 400")
    void badRequests() throws Exception
    {
        // Given invalid payloads
        // When posting
        // Then each maps to 400
        mockMvc.perform(post("/search").contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":-91.0,\"longitude\":-58.3816}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/search").contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":-34.6037,\"longitude\":-181.0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/search").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/search").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/search").contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":NaN,\"longitude\":-58.3816}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET on the search path returns 405")
    void methodNotAllowed() throws Exception
    {
        // Given a GET against the POST-only path
        // When requesting
        // Then the method mapping rejects it
        mockMvc.perform(get("/search")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("wrong content type returns 415")
    void unsupportedMediaType() throws Exception
    {
        // Given a non-JSON body
        // When posting
        // Then the consumes contract rejects it
        mockMvc.perform(post("/search").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("missing request id still returns 200 with a generated UUID")
    void missingRequestIdGeneratesUuid() throws Exception
    {
        // Given a service result and no incoming correlation id
        ArbolResponse dto = new ArbolResponse(123, "Jacaranda mimosifolia", 8, 30, 1, -58.3816, -34.6037);
        when(service.findNearby(any(SearchRequest.class)))
                .thenReturn(new SearchResponse(List.of(dto), 1, false, -34.604, -58.382, 1000));

        // When posting without the header
        MvcResult result = mockMvc.perform(post("/search").contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":-34.6037,\"longitude\":-58.3816}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then a valid UUID echoes on the response
        assertThat(UUID.fromString(result.getResponse().getHeader("X-Request-Id"))).isNotNull();
    }

    @Test
    @DisplayName("unknown path returns a 404 ProblemDetail, not the Boot default")
    void unknownPathIs404ProblemDetail() throws Exception
    {
        // Given no mapping for the path
        // When requesting
        // Then the contract stays RFC 9457 with a 404 status
        mockMvc.perform(get("/unknown-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Mongo failure passes through as 503")
    void mongoFailureIs503() throws Exception
    {
        // Given a Mongo outage behind the service
        when(service.findNearby(any(SearchRequest.class)))
                .thenThrow(new DataAccessResourceFailureException("down"));

        // When posting a valid search
        // Then the outage surfaces as 503, never 500
        mockMvc.perform(post("/search").contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":-34.6037,\"longitude\":-58.3816}"))
                .andExpect(status().isServiceUnavailable());
    }
}
