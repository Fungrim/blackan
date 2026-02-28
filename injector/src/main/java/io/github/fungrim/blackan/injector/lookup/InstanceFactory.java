package io.github.fungrim.blackan.injector.lookup;

import java.util.Collection;

import org.jboss.jandex.ClassInfo;

public interface InstanceFactory {

    public LimitedInstance create(RecursionKey key, Collection<ClassInfo> filteredCandidates);
    
    public void close();

}
