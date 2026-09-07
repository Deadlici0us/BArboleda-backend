package com.barboleda.arbolado.web;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.barboleda.arbolado.domain.SearchRequest;
import com.barboleda.arbolado.domain.SearchResponse;
import com.barboleda.arbolado.service.ArbolSearchFacade;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Search endpoint: thin delegation to {@link ArbolSearchFacade}, JSON in and out.
 */
@Tag(name = "search", description = "Geospatial tree search over the Buenos Aires dataset")
@RestController
public class ArbolController
{

    private final ArbolSearchFacade service;

    /**
     * Builds the controller over the search facade.
     *
     * @param service the search facade
     */
    public ArbolController(ArbolSearchFacade service)
    {
        this.service = service;
    }

    /**
     * Searches trees near a center point.
     *
     * @param request the validated search input
     * @return the wrapped, distance-sorted result
     */
    @Operation(summary = "Search trees near a center point",
            description = "Returns registry trees within the request radius, distance-sorted ascending")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distance-sorted wrapper, possibly empty",
                    content = @Content(schema = @Schema(implementation = SearchResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid coordinates or radius",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))})
    @PostMapping(path = "/search", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SearchResponse> search(@Valid @RequestBody SearchRequest request)
    {
        return ResponseEntity.ok(service.findNearby(request));
    }
}
