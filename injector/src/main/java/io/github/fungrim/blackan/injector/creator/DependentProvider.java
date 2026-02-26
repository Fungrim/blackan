package io.github.fungrim.blackan.injector.creator;

import java.util.HashSet;
import java.util.Set;

import io.github.fungrim.blackan.injector.Context;
import jakarta.inject.Provider;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DependentProvider<T> implements Provider<T> {

    private static final ThreadLocal<Set<Class<?>>> CONSTRUCTION_STACK = ThreadLocal.withInitial(HashSet::new);

    private final Context context;
    private final Class<T> clazz;

    @Override
    public T get() {
        Set<Class<?>> stack = CONSTRUCTION_STACK.get();
        if (!stack.add(clazz)) {
            throw new ConstructionException("Circular dependency detected while constructing: " + clazz.getName());
        }
        try {
            T instance = ConstructorInvocation.of(context, clazz).create();
            FieldInvocation.of(context, instance).forEach(FieldInvocation::set);
            MethodInvocation.of(context, instance).forEach(MethodInvocation::invoke);
            return instance;
        } finally {
            stack.remove(clazz);
        }
    }
}
