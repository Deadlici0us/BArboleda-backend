package com.barboleda.arbolado;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point for the arbolado-backend geospatial tree search service.
 */
@SpringBootApplication
@EnableCaching
public class ArboladoApplication
{

    /**
     * Boots the Spring application context.
     *
     * @param args command-line arguments, unused
     */
    public static void main(String[] args)
    {
        SpringApplication.run(ArboladoApplication.class, args);
    }
}
