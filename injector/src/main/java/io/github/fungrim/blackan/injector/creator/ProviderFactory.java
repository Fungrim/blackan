package io.github.fungrim.blackan.injector.creator;

import java.util.Optional;

import org.jboss.jandex.ClassInfo;

import jakarta.inject.Provider;

public interface ProviderFactory {

    public default <T> Provider<T> create(ClassInfo type, Class<T> clazzType) {
        return create(type, Optional.empty(), clazzType);
    }

    public <T> Provider<T> create(ClassInfo type, Optional<InjectionLocation<T>> location, Class<T> clazzType);

    void close();

}
