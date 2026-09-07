package com.barboleda.arbolado.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * A Buenos Aires street tree as stored by {@code arbolado-db/etl.py}.
 *
 * <p>Snake_case Mongo keys map onto camelCase fields via {@code @Field}. Coordinates
 * live twice: the {@code location} GeoJSON point (queried via the out-of-band
 * {@code 2dsphere} index owned by the ETL/infra — this app never creates indexes)
 * and the plain {@code long}/{@code lat} ETL doubles (served in the API).
 */
@Document("arboles")
public class Arbol
{

    @Id
    private String id;

    private GeoJsonPoint location;

    @Field("nro_registro")
    private Integer nroRegistro;

    @Field("nombre_cientifico")
    private String nombreCientifico;

    @Field("altura_arbol")
    private Integer alturaArbol;

    @Field("diametro_altura_pecho")
    private Integer diametroAlturaPecho;

    @Field("comuna")
    private Integer comuna;

    @Field("long")
    private Double longitude;

    @Field("lat")
    private Double latitude;

    @Field("source")
    private String source;

    @Field("es_merged")
    private Boolean esMerged;

    /**
     * Persistence constructor used by Spring Data Mongo when reading documents.
     */
    public Arbol()
    {
    }

    /**
     * Builds a fully populated tree.
     *
     * @param id tree document id
     * @param location GeoJSON point in lon-first order, may be null
     * @param nroRegistro registry number, may be null
     * @param nombreCientifico scientific name, may be null
     * @param alturaArbol tree height, may be null
     * @param diametroAlturaPecho diameter at breast height, may be null
     * @param comuna district number, may be null
     * @param longitude ETL longitude, may be null
     * @param latitude ETL latitude, may be null
     * @param source dataset source tag, may be null
     * @param esMerged merged-record flag, may be null
     */
    public Arbol(String id, GeoJsonPoint location, Integer nroRegistro, String nombreCientifico, Integer alturaArbol,
            Integer diametroAlturaPecho, Integer comuna, Double longitude, Double latitude, String source,
            Boolean esMerged)
    {
        this.id = id;
        this.location = location;
        this.nroRegistro = nroRegistro;
        this.nombreCientifico = nombreCientifico;
        this.alturaArbol = alturaArbol;
        this.diametroAlturaPecho = diametroAlturaPecho;
        this.comuna = comuna;
        this.longitude = longitude;
        this.latitude = latitude;
        this.source = source;
        this.esMerged = esMerged;
    }

    /**
     * Returns the document id.
     *
     * @return the id, null for transient instances
     */
    public String getId()
    {
        return id;
    }

    /**
     * Returns the GeoJSON query point.
     *
     * @return the location, null when the record has none
     */
    public GeoJsonPoint getLocation()
    {
        return location;
    }

    /**
     * Returns the registry number.
     *
     * @return the registry number, may be null
     */
    public Integer getNroRegistro()
    {
        return nroRegistro;
    }

    /**
     * Returns the scientific name.
     *
     * @return the scientific name, may be null
     */
    public String getNombreCientifico()
    {
        return nombreCientifico;
    }

    /**
     * Returns the tree height.
     *
     * @return the height, may be null
     */
    public Integer getAlturaArbol()
    {
        return alturaArbol;
    }

    /**
     * Returns the diameter at breast height.
     *
     * @return the diameter, may be null
     */
    public Integer getDiametroAlturaPecho()
    {
        return diametroAlturaPecho;
    }

    /**
     * Returns the district number.
     *
     * @return the district, may be null
     */
    public Integer getComuna()
    {
        return comuna;
    }

    /**
     * Returns the ETL longitude.
     *
     * @return the longitude, may be null
     */
    public Double getLongitude()
    {
        return longitude;
    }

    /**
     * Returns the ETL latitude.
     *
     * @return the latitude, may be null
     */
    public Double getLatitude()
    {
        return latitude;
    }

    /**
     * Returns the dataset source tag.
     *
     * @return the source, may be null
     */
    public String getSource()
    {
        return source;
    }

    /**
     * Returns the merged-record flag.
     *
     * @return the flag, may be null
     */
    public Boolean getEsMerged()
    {
        return esMerged;
    }
}
