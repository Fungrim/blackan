package io.github.fungrim.blackan.injector.creator;

import org.jboss.jandex.ClassInfo;

import io.github.fungrim.blackan.common.api.Extension;
import io.github.fungrim.blackan.injector.Context;
import jakarta.inject.Provider;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DependentProvider<T> implements Provider<T> {

    private final Context context;
    private final ClassInfo classInfo;
    private final Class<T> clazz;

    @Override
    public T get() {
        return DependentProviderStack.exec(clazz, isDirectExtension(), () -> {
            T instance = ConstructorInvocation.of(context, clazz).create();
            FieldInvocation.of(context, instance).forEach(FieldInvocation::set);
            MethodInvocation.of(context, instance).forEach(MethodInvocation::invoke);
            BeanLifecycle.invokePostConstruct(instance);
            context.destroyableTracker().register(instance);
            return instance;
        });
    }

    private boolean isDirectExtension() {
        return classInfo.hasAnnotation(Extension.class);
    }
}
