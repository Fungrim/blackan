package io.github.fungrim.blackan.injector.creator;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.common.cdi.InjectionTarget;
import io.github.fungrim.blackan.common.cdi.TargetAwareProvider;
import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.lookup.RecursionKey;
import io.github.fungrim.blackan.injector.producer.ProducerCacheKey;
import io.github.fungrim.blackan.injector.producer.ProducerRegistry;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Provider;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InvocationUtil {

    public static Object[] resolveParameters(Context context, RecursionKey[] parameters, Type[] genericTypes) {
        return resolveParameters(context, parameters, genericTypes, null);
    }

    public static Object[] resolveParameters(Context context, RecursionKey[] parameters, Type[] genericTypes, InjectionTarget[] targets) {
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            InjectionTarget target = targets != null ? targets[i] : null;
            args[i] = resolveInjectionPoint(context, parameters[i], genericTypes[i], target);
        }
        return args;
    }

    public static Object resolveInjectionPoint(Context context, RecursionKey key, Type genericType) {
        return resolveInjectionPoint(context, key, genericType, null);
    }

    @SuppressWarnings("unchecked")
    public static Object resolveInjectionPoint(Context context, RecursionKey key, Type genericType, InjectionTarget target) {
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
        if (Context.class.isAssignableFrom(rawType)) {
            if(DependentProviderStack.isInExtension()) {
                return context;
            } else {
                throw new ConstructionException("Injection of Context is disallowed outside of extensions");
            }
        }
        Annotation[] qualifiers = key.qualifiers().toArray(new Annotation[0]);
        ProducerCacheKey producerKey = ProducerCacheKey.of(genericType, qualifiers);
        ProducerRegistry registry = context.producerRegistry();
        if (registry != null) {
            Optional<Provider<Object>> producer = registry.find(producerKey);
            if (producer.isPresent()) {
                return unwrapIfTargetAware(producer.get().get(), target);
            }
        }
        return unwrapIfTargetAware(
                context.getInstance(key.type()).get(context.loadClass(key.type())),
                target
        );
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

    private static Object unwrapIfTargetAware(Object value, InjectionTarget target) {
        if (value instanceof TargetAwareProvider<?> tap && target != null) {
            return tap.get(target);
        }
        return value;
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
