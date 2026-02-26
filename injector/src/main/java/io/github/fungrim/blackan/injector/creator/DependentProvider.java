package io.github.fungrim.blackan.injector.creator;

import io.github.fungrim.blackan.injector.Context;
import jakarta.inject.Provider;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DependentProvider<T> implements Provider<T> {

    private final Context context;
    private final Class<T> clazz;

    @Override
    public T get() {
        T instance = ConstructorInvocation.of(context, clazz).create();
        FieldInvocation.of(context, instance).forEach(FieldInvocation::set);
        MethodInvocation.of(context, instance).forEach(MethodInvocation::invoke);
        return instance;
    }
}
