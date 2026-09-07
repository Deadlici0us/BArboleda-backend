package com.barboleda.arbolado.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Pins the exact JSON wire contract from PLAN.md #3.
 */
class JsonContractTest
{

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("search request serializes camelCase")
    void requestIsCamelCase() throws Exception
    {
        // Given a request
        SearchRequest request = new SearchRequest(-34.6037, -58.3816, 500.0);

        // When serializing
        String json = objectMapper.writeValueAsString(request);

        // Then the locked camelCase payload results
        assertThat(json).isEqualTo("{\"latitude\":-34.6037,\"longitude\":-58.3816,\"radius\":500.0}");
    }

    @Test
    @DisplayName("tree item serializes snake_case matching the ETL keys")
    void responseIsSnakeCase() throws Exception
    {
        // Given a fully populated response DTO
        ArbolResponse response = new ArbolResponse(123, "Jacaranda mimosifolia", 8, 30, 1, -58.3816, -34.6037);

        // When serializing
        String json = objectMapper.writeValueAsString(response);

        // Then the locked snake_case payload results
        assertThat(json).isEqualTo("{\"nro_registro\":123,\"nombre_cientifico\":\"Jacaranda mimosifolia\","
                + "\"altura_arbol\":8,\"diametro_altura_pecho\":30,\"comuna\":1,"
                + "\"long\":-58.3816,\"lat\":-34.6037}");
    }

    @Test
    @DisplayName("empty entity uses the persistence no-arg constructor with null fields")
    void noArgConstructorDefaults() throws Exception
    {
        // Given an entity built the way Spring Data Mongo instantiates reads
        Arbol entity = new Arbol();

        // When inspecting
        // Then every field defaults to null
        assertThat(entity.getId()).isNull();
        assertThat(entity.getLocation()).isNull();
    }
}
