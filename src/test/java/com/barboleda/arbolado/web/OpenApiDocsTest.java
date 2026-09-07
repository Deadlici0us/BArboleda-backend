package com.barboleda.arbolado.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * OpenAPI contract: springdoc serves the search operation with a documented summary.
 *
 * <p>Full context (not a {@code @WebMvcTest} slice) because springdoc registers its
 * controllers via auto-configuration, which the slice does not pick up.
 */
@SpringBootTest(properties = {
        "MONGO_URI=mongodb://localhost:27017/arbolado_db",
        "REDIS_HOST=localhost",
        "REDIS_PASSWORD=",
        "REDIS_SSL=false"})
@AutoConfigureMockMvc
class OpenApiDocsTest
{

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MongoTemplate mongoTemplate;

    @MockBean
    private GridFsTemplate gridFsTemplate;

    @Test
    @DisplayName("api-docs exposes POST /search with a summary and radius 80 example")
    void searchOperationIsDocumented() throws Exception
    {
        // Given the documented controller
        // When fetching the OpenAPI document
        // Then the search operation carries its summary and the radius example
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").isString())
                .andExpect(jsonPath("$.paths['/search'].post").exists())
                .andExpect(jsonPath("$.paths['/search'].post.summary").isNotEmpty())
                .andExpect(jsonPath("$.components.schemas.SearchRequest.properties.radius.example").exists())
                .andExpect(jsonPath("$.components.schemas.SearchRequest.properties.radius.example").value("80"))
                // Correct response structure: wrapper with trimmed item schema (no source/es_merged)
                .andExpect(jsonPath("$.components.schemas.SearchResponse.properties.items.items.$ref").exists())
                .andExpect(jsonPath("$.components.schemas.ArbolResponse.properties.source").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ArbolResponse.properties.es_merged").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ArbolResponse.required").exists())
                .andExpect(jsonPath("$.components.schemas.ArbolResponse.properties.nro_registro.default").value(0))
                // Description leak fix: ArbolResponse must have its own identity, never inherit items description
                .andExpect(jsonPath("$.components.schemas.ArbolResponse.description")
                        .value("Single street tree response"))
                .andExpect(jsonPath("$.components.schemas.SearchResponse.properties.items.description")
                        .value("Distance-sorted page, ascending"))
                // 400 carries no response body schema (only description, no content schema reference)
                .andExpect(jsonPath("$.paths['/search'].post.responses.400" +
                        ".content.application/json.schema").doesNotExist());
    }
}
