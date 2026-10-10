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
        // Intentionally empty: Spring Data Mongo instantiates reads via reflection.
    }

    /**
     * Builds a fully populated tree.
     *
     * @return a fresh builder with every field unset
     */
    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Step builder for fully populated trees (tests and fixtures).
     *
     * <p>Replaces the former 11-parameter constructor: one fluent call per field
     * instead of one positional argument list.
     */
    public static final class Builder
    {

        private String id;

        private GeoJsonPoint location;

        private Integer nroRegistro;

        private String nombreCientifico;

        private Integer alturaArbol;

        private Integer diametroAlturaPecho;

        private Integer comuna;

        private Double longitude;

        private Double latitude;

        private String source;

        private Boolean esMerged;

        private Builder()
        {
        }

        /**
         * Sets the document id.
         *
         * @param id the id
         * @return this builder
         */
        public Builder id(String id)
        {
            this.id = id;
            return this;
        }

        /**
         * Sets the GeoJSON query point.
         *
         * @param location the location, may be null
         * @return this builder
         */
        public Builder location(GeoJsonPoint location)
        {
            this.location = location;
            return this;
        }

        /**
         * Sets the registry number.
         *
         * @param nroRegistro the registry number, may be null
         * @return this builder
         */
        public Builder nroRegistro(Integer nroRegistro)
        {
            this.nroRegistro = nroRegistro;
            return this;
        }

        /**
         * Sets the scientific name.
         *
         * @param nombreCientifico the name, may be null
         * @return this builder
         */
        public Builder nombreCientifico(String nombreCientifico)
        {
            this.nombreCientifico = nombreCientifico;
            return this;
        }

        /**
         * Sets the tree height.
         *
         * @param alturaArbol the height, may be null
         * @return this builder
         */
        public Builder alturaArbol(Integer alturaArbol)
        {
            this.alturaArbol = alturaArbol;
            return this;
        }

        /**
         * Sets the diameter at breast height.
         *
         * @param diametroAlturaPecho the diameter, may be null
         * @return this builder
         */
        public Builder diametroAlturaPecho(Integer diametroAlturaPecho)
        {
            this.diametroAlturaPecho = diametroAlturaPecho;
            return this;
        }

        /**
         * Sets the district number.
         *
         * @param comuna the district, may be null
         * @return this builder
         */
        public Builder comuna(Integer comuna)
        {
            this.comuna = comuna;
            return this;
        }

        /**
         * Sets the ETL longitude.
         *
         * @param longitude the longitude, may be null
         * @return this builder
         */
        public Builder longitude(Double longitude)
        {
            this.longitude = longitude;
            return this;
        }

        /**
         * Sets the ETL latitude.
         *
         * @param latitude the latitude, may be null
         * @return this builder
         */
        public Builder latitude(Double latitude)
        {
            this.latitude = latitude;
            return this;
        }

        /**
         * Sets the dataset source tag.
         *
         * @param source the source, may be null
         * @return this builder
         */
        public Builder source(String source)
        {
            this.source = source;
            return this;
        }

        /**
         * Sets the merged-record flag.
         *
         * @param esMerged the flag, may be null
         * @return this builder
         */
        public Builder esMerged(Boolean esMerged)
        {
            this.esMerged = esMerged;
            return this;
        }

        /**
         * Builds the tree from the collected fields.
         *
         * @return a fully populated tree
         */
        public Arbol build()
        {
            Arbol tree = new Arbol();
            tree.id = id;
            tree.location = location;
            tree.nroRegistro = nroRegistro;
            tree.nombreCientifico = nombreCientifico;
            tree.alturaArbol = alturaArbol;
            tree.diametroAlturaPecho = diametroAlturaPecho;
            tree.comuna = comuna;
            tree.longitude = longitude;
            tree.latitude = latitude;
            tree.source = source;
            tree.esMerged = esMerged;
            return tree;
        }
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
