package io.github.fungrim.blackan.injector.creator;

import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.context.ScopeRegistry;
import jakarta.inject.Provider;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SubScopeProvider<T> implements Provider<T> {

    private final ScopeRegistry scopeRegistry;
    private final DotName type;
    private final Class<T> clazz;

    @Override
    public T get() {
        return current().getInstance(type).get(clazz);
    }

    private Context current() {
        return scopeRegistry.current().orElseThrow(() -> new IllegalStateException("No current context found"));
    }
}
