package io.github.fungrim.blackan.common.cdi;

public interface TargetAwareProvider<T> {

    T get(InjectionPoint target, boolean isOptional);
    
}
