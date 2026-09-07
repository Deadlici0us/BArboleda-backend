package com.barboleda.arbolado.service;

import org.springframework.stereotype.Component;

import com.barboleda.arbolado.domain.Arbol;
import com.barboleda.arbolado.domain.ArbolResponse;

/**
 * Maps persistence entities onto API DTOs. Never exposes {@link Arbol} past this boundary.
 */
@Component
public class ArbolMapper
{

    /**
     * Converts one tree entity into its response DTO.
     *
     * <p>Null numeric fields coalesce to {@code 0} or {@code 0.0}, strings to {@code ""}.
     *
     * @param entity the persisted tree, with lon-first {@code location} (x is longitude)
     * @return the DTO with plain {@code long}/{@code lat} doubles, defaults when absent
     */
    public ArbolResponse toResponse(Arbol entity)
    {
        int nroRegistro = (entity.getNroRegistro() != null) ? entity.getNroRegistro() : 0;
        String nombreCientifico = (entity.getNombreCientifico() != null) ? entity.getNombreCientifico() : "";
        int alturaArbol = (entity.getAlturaArbol() != null) ? entity.getAlturaArbol() : 0;
        int diametroAlturaPecho = (entity.getDiametroAlturaPecho() != null) ? entity.getDiametroAlturaPecho() : 0;
        int comuna = (entity.getComuna() != null) ? entity.getComuna() : 0;

        double longitude = 0.0;
        double latitude = 0.0;
        if (entity.getLocation() != null)
        {
            longitude = entity.getLocation().getX();
            latitude = entity.getLocation().getY();
        }

        return new ArbolResponse(nroRegistro, nombreCientifico, alturaArbol,
                diametroAlturaPecho, comuna, longitude, latitude);
    }
}
