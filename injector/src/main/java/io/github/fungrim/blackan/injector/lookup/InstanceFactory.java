package io.github.fungrim.blackan.injector.lookup;

import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.injector.context.DecoratedInstance;

public interface InstanceFactory {

    public <T> DecoratedInstance<T> decorate(T instance);

    public LimitedInstance create(RecursionKey key);

    void evict(DotName type);
    
    public void close();

}
