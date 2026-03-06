package io.github.fungrim.blackan.injector.lookup;

import jakarta.enterprise.inject.Instance.Handle;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Provider;

class LocalHandle<T> implements Handle<T> {

    private final Provider<T> provider;
    private T instance;
    private boolean obtained;

    LocalHandle(Provider<T> provider) {
        this.provider = provider;
    }

    @Override
    public T get() {
        if (!obtained) {
            instance = provider.get();
            obtained = true;
        }
        return instance;
    }

    @Override
    public Bean<T> getBean() {
        throw new UnsupportedOperationException("Bean metadata not available in this context");
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException("Bean metadata not available in this context");
    }

    @Override
    public void close() { }
}
