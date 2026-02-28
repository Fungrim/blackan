package io.github.fungrim.blackan.injector.lookup;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.injector.creator.ProviderFactory;

public class CachingInstanceFactory implements InstanceFactory{

    private final ProviderFactory creatorFactory;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<RecursionKey, RecursiveInstance> cache = new HashMap<>();

    public CachingInstanceFactory(ProviderFactory creatorFactory) {
        this.creatorFactory = creatorFactory;
    }

    @Override
    public LimitedInstance create(RecursionKey key, Collection<ClassInfo> filteredCandidates) {
        lock.writeLock().lock();
        try {
            return cache.computeIfAbsent(key, k -> new RecursiveInstance(key, filteredCandidates, creatorFactory, this));
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
