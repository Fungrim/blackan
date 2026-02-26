package io.github.fungrim.blackan.injector.lookup;

import java.lang.annotation.Annotation;
import java.util.Iterator;

import org.jboss.jandex.ClassInfo;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Provider;

public interface LimitedInstance {

    public Iterator<ClassInfo> candidates();

    public <T> T get(Class<T> type);

    public <T> Provider<T> toProvider(Class<T> type);

    public ClassInfo getCandidate();

    public boolean isResolvable();

    public boolean isAmbiguous();

    public boolean isUnsatisfied();
    
    public LimitedInstance select(Annotation... qualifiers);

    public <T> Instance<T> toInstance(Class<T> type);
    
}
