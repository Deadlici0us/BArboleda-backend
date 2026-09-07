package com.barboleda.arbolado.config;

import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Warms the Mongo pool once the app is ready.
 *
 * <p>Shifts the cold TLS + AUTH cost from the first user request to the
 * deploy, where the readiness probe absorbs it. Fail-open: a failed ping only
 * logs, the readiness probe reports Atlas reachability instead.
 */
@Component
@ConditionalOnProperty(name = "app.mongo.warmup.enabled", havingValue = "true", matchIfMissing = true)
public class MongoWarmup
{

    private static final Logger log = LoggerFactory.getLogger(MongoWarmup.class);

    private final MongoTemplate mongoTemplate;

    /**
     * Builds the warmup over the shared template.
     *
     * @param mongoTemplate the configured template, never null
     */
    public MongoWarmup(MongoTemplate mongoTemplate)
    {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Pings Atlas once the context is ready so the pool holds a live connection.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmup()
    {
        long start = System.nanoTime();
        try
        {
            mongoTemplate.executeCommand(new Document("ping", 1));
            long rttMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            log.info("Mongo warmup ping completed rtt_ms={}", rttMs);
        }
        catch (RuntimeException failure)
        {
            log.warn("Mongo warmup ping failed ({}); readiness probe reports it", failure.toString());
        }
    }
}
