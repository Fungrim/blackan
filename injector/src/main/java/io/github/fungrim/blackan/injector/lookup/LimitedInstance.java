package io.github.fungrim.blackan.injector.lookup;

import java.lang.annotation.Annotation;
import java.util.List;

import org.jboss.jandex.ClassInfo;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Provider;

public interface LimitedInstance {

    // ordered list of candidates
    public List<ClassInfo> candidates();

    public <T> T get(Class<T> type);

    public <T> Provider<T> toProvider(Class<T> type);

    public ClassInfo getCandidate();

    public boolean isResolvable();

    public boolean isAmbiguous();

    public boolean isUnsatisfied();
    
    public LimitedInstance select(Annotation... qualifiers);

    public <T> Instance<T> toInstance(Class<T> type);
    
}
