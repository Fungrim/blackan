package io.github.fungrim.blackan.injector.lookup;

import java.util.Collection;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

public interface InstanceFactory {

    public LimitedInstance create(RecursionKey key, Collection<ClassInfo> filteredCandidates);

    void evict(DotName type);
    
    public void close();

}
