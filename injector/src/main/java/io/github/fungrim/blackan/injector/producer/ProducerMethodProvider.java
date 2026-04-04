package io.github.fungrim.blackan.injector.producer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.creator.ConstructionException;
import io.github.fungrim.blackan.injector.creator.InjectionPointResolver;
import io.github.fungrim.blackan.injector.lookup.InjectionPointLookupKey;
import jakarta.inject.Provider;

public class ProducerMethodProvider<T> implements Provider<T> {

    private final Context context;
    private final Class<?> declaringClass;
    private final Method method;
    private final InjectionPointLookupKey[] parameterKeys;
    private final Type[] genericParameterTypes;

    public ProducerMethodProvider(Context context, Class<?> declaringClass, Method method) {
        this.context = context;
        this.declaringClass = declaringClass;
        this.method = method;
        this.parameterKeys = extractParameterKeys(method);
        this.genericParameterTypes = method.getGenericParameterTypes();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        try {
            Object owner = context.get(declaringClass);
            Object[] args = InjectionPointResolver.resolveParameters(context, parameterKeys, genericParameterTypes);
            method.setAccessible(true);
            T t = (T) method.invoke(owner, args);
            method.setAccessible(false);
            return t;
        } catch (Exception e) {
            throw new ConstructionException(
                    "Failed to invoke producer method " + declaringClass.getName() + "." + method.getName(), e);
        }
    }

    private static InjectionPointLookupKey[] extractParameterKeys(Method method) {
        InjectionPointLookupKey[] keys = new InjectionPointLookupKey[method.getParameterCount()];
        for (int i = 0; i < method.getParameterCount(); i++) {
            Class<?> parameterType = method.getParameterTypes()[i];
            Annotation[] annotations = method.getParameterAnnotations()[i];
            keys[i] = InjectionPointLookupKey.of(parameterType, annotations);
        }
        return keys;
    }
}
