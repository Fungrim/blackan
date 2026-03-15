package io.github.fungrim.blackan.injector.util;

import java.util.concurrent.Callable;

@FunctionalInterface
public interface SafeCallable<T> {

    public static <T> SafeCallable<T> of(Callable<T> callable) {
        return () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    T call();
    
}
