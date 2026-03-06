package io.github.fungrim.blackan.injector.lookup;

import org.jboss.jandex.DotName;

public interface InstanceFactory {

    public LimitedInstance create(RecursionKey key);

    void evict(DotName type);
    
    public void close();

}
