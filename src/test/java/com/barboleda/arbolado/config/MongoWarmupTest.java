package com.barboleda.arbolado.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Warmup ping contract without any server.
 *
 * <p>The ping shifts the cold TLS cost from the first user request to the
 * deploy, where the readiness probe absorbs it.
 */
class MongoWarmupTest
{

    @Test
    @DisplayName("warmup sends a single ping through the shared template")
    void warmupSendsSinglePing()
    {
        // Given a mocked template and the warmup over it
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        when(mongoTemplate.executeCommand(argThat((Document doc) -> doc != null && Integer.valueOf(1).equals(
                doc.get("ping"))))).thenReturn(new Document("ok", 1));
        MongoWarmup warmup = new MongoWarmup(mongoTemplate);

        // When warming up on boot
        warmup.warmup();

        // Then exactly one ping went through
        verify(mongoTemplate, times(1)).executeCommand(
                argThat((Document doc) -> Integer.valueOf(1).equals(doc.get("ping"))));
    }

    @Test
    @DisplayName("warmup failure never fails the boot")
    void warmupFailureDoesNotPropagate()
    {
        // Given a template whose ping throws
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        when(mongoTemplate.executeCommand(any(Document.class)))
                .thenThrow(new RuntimeException("atlas unreachable"));
        MongoWarmup warmup = new MongoWarmup(mongoTemplate);

        // When warming up on boot
        // Then the failure is swallowed (readiness probe reports it instead)
        assertThatCode(warmup::warmup).doesNotThrowAnyException();
    }
}
