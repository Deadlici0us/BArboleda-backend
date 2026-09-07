package com.barboleda.arbolado.config;

import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Warms the Mongo pool once the app is ready.
 */
@Component
@ConditionalOnProperty(name = "app.mongo.warmup.enabled", havingValue = "true", matchIfMissing = true)
public class MongoWarmup extends AbstractWarmup
{

    private final MongoTemplate mongoTemplate;

    public MongoWarmup(MongoTemplate mongoTemplate)
    {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmup()
    {
        timedPing(() -> mongoTemplate.executeCommand(new Document("ping", 1)), "Mongo");
    }
}
