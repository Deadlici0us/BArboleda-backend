package com.barboleda.arbolado.config;

import org.springframework.cache.Cache;

/**
 * Abstract base for cache decorators that only override selected methods,
 * removing the boilerplate of forwarding every call to the delegate.
 */
public abstract class ForwardingCache implements Cache
{

    protected final Cache delegate;

    protected ForwardingCache(Cache delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public String getName()
    {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache()
    {
        return delegate.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key)
    {
        return delegate.get(key);
    }

    @Override
    public <T> T get(Object key, Class<T> type)
    {
        return delegate.get(key, type);
    }

    @Override
    public void put(Object key, Object value)
    {
        delegate.put(key, value);
    }

    @Override
    public void evict(Object key)
    {
        delegate.evict(key);
    }

    @Override
    public void clear()
    {
        delegate.clear();
    }
}
