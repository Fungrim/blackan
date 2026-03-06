package io.github.fungrim.blackan.injector.lookup;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.github.fungrim.blackan.injector.creator.ProviderFactory;

public class CachingInstanceFactory implements InstanceFactory{

    private final ProviderFactory creatorFactory;
    private final IndexView index;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<RecursionKey, RecursiveInstance> cache = new HashMap<>();

    public CachingInstanceFactory(ProviderFactory creatorFactory, IndexView index) {
        this.creatorFactory = creatorFactory;
        this.index = index;
    }

    @Override
    public LimitedInstance create(RecursionKey key) {
        lock.writeLock().lock();
        try {
            return cache.computeIfAbsent(key, k -> new RecursiveInstance(key, creatorFactory, this, index));
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void evict(DotName type) {
        lock.writeLock().lock();
        try {
            cache.entrySet().removeIf(e -> e.getKey().type().equals(type));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void close() {
        cache.clear();
    }
}
