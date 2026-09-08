package com.barboleda.arbolado.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * Two-tier compression: wraps an inner JSON serializer with GZIP for Redis values.
 * Shrinks worst-case payloads from ~628 KB down to ~90 KB.
 */
public class GzipRedisSerializer implements RedisSerializer<Object>
{

    private final RedisSerializer<Object> delegate;

    public GzipRedisSerializer(RedisSerializer<Object> delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public byte[] serialize(Object object) throws SerializationException
    {
        try
        {
            byte[] raw = delegate.serialize(object);
            if (raw == null || raw.length == 0)
            {
                return raw;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length);
            try (GZIPOutputStream gzip = new GZIPOutputStream(out))
            {
                gzip.write(raw);
            }
            return out.toByteArray();
        }
        catch (IOException e)
        {
            throw new SerializationException("GZIP serialize failed", e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException
    {
        if (bytes == null || bytes.length == 0)
        {
            return delegate.deserialize(bytes);
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                GZIPInputStream gzip = new GZIPInputStream(in))
        {
            byte[] raw = gzip.readAllBytes();
            return delegate.deserialize(raw);
        }
        catch (IOException e)
        {
            throw new SerializationException("GZIP deserialize failed", e);
        }
    }
}
