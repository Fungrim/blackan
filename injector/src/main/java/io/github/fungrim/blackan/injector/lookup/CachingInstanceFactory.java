package io.github.fungrim.blackan.injector.lookup;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.github.fungrim.blackan.injector.context.DecoratedInstance;
import io.github.fungrim.blackan.injector.creator.ProviderFactory;

public class CachingInstanceFactory implements InstanceFactory{

    private final ProviderFactory creatorFactory;
    private final IndexView index;
    private final Set<DotName> vetoedTypes;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<RecursionKey, RecursiveInstance> cache = new HashMap<>();

    public CachingInstanceFactory(ProviderFactory creatorFactory, IndexView index, Set<DotName> vetoedTypes) {
        this.creatorFactory = creatorFactory;
        this.index = index;
        this.vetoedTypes = vetoedTypes;
    }

    @Override
    public LimitedInstance create(RecursionKey key) {
        lock.writeLock().lock();
        try {
            return cache.computeIfAbsent(key, k -> new RecursiveInstance(key, creatorFactory, this, index, vetoedTypes));
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public <T> DecoratedInstance<T> decorate(T instance) {
        return creatorFactory.decorate(instance);
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
