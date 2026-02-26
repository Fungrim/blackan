package io.github.fungrim.blackan.injector.creator;

import jakarta.inject.Provider;

public class SingletonProvider<T> implements Provider<T> {

    private T instance;
    private final Provider<T> wrapped;

    public SingletonProvider(Provider<T> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public synchronized T get() {
        if(instance == null) {
            instance = wrapped.get();
        }
        return instance;
    }
}
