package com.barboleda.arbolado.config;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base for connection warm-up components: eliminates the duplicated
 * timed-ping + fail-open log pattern across Mongo and Redis.
 */
public abstract class AbstractWarmup
{

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Runs a timed ping with fail-open logging.
     *
     * @param ping the ping operation
     * @param operationName the operation label for logs
     */
    protected void timedPing(Runnable ping, String operationName)
    {
        long start = System.nanoTime();
        try
        {
            ping.run();
            long rttMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            log.info("{} warmup ping completed rtt_ms={}", operationName, rttMs);
        }
        catch (RuntimeException failure)
        {
            log.warn("{} warmup ping failed ({}); stays fail-open", operationName, failure.toString());
        }
    }
}
