package io.github.fungrim.blackan.injector.creator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Optional;

import io.github.fungrim.blackan.common.cdi.InjectionPoint;
import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.lookup.InjectionPointLookupKey;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConstructorInvocation<T> {

    public static <T> ConstructorInvocation<T> of(Context context, Class<T> clazz) {
        Constructor<?> constructor = findInjectConstructor(clazz).orElseGet(() -> findDefaultConstructor(clazz).orElseThrow(() -> new ConstructionException(String.format("No constructor found for %s", clazz.getName()))));
        return new ConstructorInvocation<>(context, constructor, extractInjectionPoints(constructor), constructor.getGenericParameterTypes());
    }

    private static Optional<Constructor<?>> findInjectConstructor(Class<?> clazz) {
        Constructor<?> constructor = null;
        for (Constructor<?> c : clazz.getConstructors()) {
            if (c.isAnnotationPresent(Inject.class)) {
                if (constructor == null) {
                    constructor = c;
                } else {
                    throw new ConstructionException(String.format("%s has multiple @Inject constructors", clazz.getName()));
                }
            }
        }
        return Optional.ofNullable(constructor);
    }

    private static Optional<Constructor<?>> findDefaultConstructor(Class<?> clazz) {
        for (Constructor<?> c : clazz.getConstructors()) {
            if (c.getParameterCount() == 0) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    private static InjectionPointLookupKey[] extractInjectionPoints(Constructor<?> constructor) {
        InjectionPointLookupKey[] keys = new InjectionPointLookupKey[constructor.getParameterCount()];
        for (int i = 0; i < constructor.getParameterCount(); i++) {
            Class<?> parameterType = constructor.getParameterTypes()[i];
            Annotation[] annotations = constructor.getParameterAnnotations()[i];
            keys[i] = InjectionPointLookupKey.of(parameterType, annotations);
        }
        return keys;
    }

    private final Context context;
    private final Constructor<?> constructor;
    private final InjectionPointLookupKey[] parameters;
    private final Type[] genericTypes;

    @SuppressWarnings("unchecked")
    public T create() {
        try {
            constructor.setAccessible(true);
            InjectionPoint[] targets = buildTargets(constructor);
            T instance = (T) constructor.newInstance(InjectionPointResolver.resolveParameters(context, parameters, genericTypes, targets));
            constructor.setAccessible(false);
            return instance;
        } catch (Exception e) {
            throw new ConstructionException("Failed to create instance of " + constructor.getDeclaringClass().getName(), e);
        }
    }

    private static InjectionPoint[] buildTargets(Constructor<?> ctor) {
        Parameter[] params = ctor.getParameters();
        InjectionPoint[] targets = new InjectionPoint[params.length];
        for (int i = 0; i < params.length; i++) {
            targets[i] = InjectionPoint.of(ctor, params[i]); 
        }
        return targets;
    }
}
