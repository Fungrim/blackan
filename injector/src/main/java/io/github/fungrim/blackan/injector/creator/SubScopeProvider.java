package io.github.fungrim.blackan.injector.creator;

import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.injector.context.ProcessScopeProvider;
import jakarta.inject.Provider;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SubScopeProvider<T> implements Provider<T> {

    private final ProcessScopeProvider scopeProvider;
    private final DotName type;
    private final Class<T> clazz;

    @Override
    public T get() {
        return scopeProvider.current().getInstance(type).get(clazz);
    }
}
