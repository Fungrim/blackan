package io.github.fungrim.blackan.injector.creator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.lookup.RecursionKey;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Provider;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InvocationUtil {

    public static Object[] resolveParameters(Context context, RecursionKey[] parameters, Type[] genericTypes) {
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            args[i] = resolveInjectionPoint(context, parameters[i], genericTypes[i]);
        }
        return args;
    }

    @SuppressWarnings("unchecked")
    public static Object resolveInjectionPoint(Context context, RecursionKey key, Type genericType) {
        Class<?> rawType = rawClass(genericType);
        if (Instance.class.isAssignableFrom(rawType)) {
            Class<Object> typeArg = (Class<Object>) extractTypeArgument(genericType);
            DotName typeName = DotName.createSimple(typeArg);
            return new SubScopeInstance<>(context.processScopeProvider(), typeName, typeArg);
        }
        if (Provider.class.isAssignableFrom(rawType)) {
            Class<Object> typeArg = (Class<Object>) extractTypeArgument(genericType);
            DotName typeName = DotName.createSimple(typeArg);
            return new SubScopeProvider<>(context.processScopeProvider(), typeName, typeArg);
        }
        return context.getInstance(key.type()).get(context.loadClass(key.type()));
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> c) {
            return c;
        }
        if (type instanceof ParameterizedType pt) {
            return (Class<?>) pt.getRawType();
        }
        return Object.class;
    }

    private static Class<?> extractTypeArgument(Type type) {
        if (type instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class<?> c) {
                return c;
            }
        }
        throw new ConstructionException("Cannot extract type argument from: " + type);
    }
}
