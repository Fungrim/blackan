package io.github.fungrim.blackan.injector.creator;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.common.cdi.InjectionPoint;
import io.github.fungrim.blackan.common.cdi.TargetAwareProvider;
import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.lookup.EventInjectionPoint;
import io.github.fungrim.blackan.injector.lookup.InjectionPointLookupKey;
import io.github.fungrim.blackan.injector.lookup.LimitedInstance;
import io.github.fungrim.blackan.injector.producer.ProducerCacheKey;
import io.github.fungrim.blackan.injector.producer.ProducerRegistry;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InjectionPointResolver {

    public static Object[] resolveParameters(Context context, InjectionPointLookupKey[] parameters, Type[] genericTypes) {
        return resolveParameters(context, parameters, genericTypes, null);
    }

    public static Object[] resolveParameters(Context context, InjectionPointLookupKey[] parameters, Type[] genericTypes, InjectionPoint[] targets) {
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            InjectionPoint target = targets != null ? targets[i] : null;
            args[i] = resolveInjectionPoint(context, parameters[i], genericTypes[i], target);
        }
        return args;
    }

    public static Object resolveInjectionPoint(Context context, InjectionPointLookupKey key, Type genericType) {
        return resolveInjectionPoint(context, key, genericType, null);
    }

    @SuppressWarnings("unchecked")
    public static Object resolveInjectionPoint(Context context, InjectionPointLookupKey key, Type genericType, InjectionPoint target) {
        Class<?> rawType = rawClass(genericType);
        if (Instance.class.isAssignableFrom(rawType)) {
            Class<Object> typeArg = (Class<Object>) extractTypeArgument(genericType);
            DotName typeName = DotName.createSimple(typeArg);
            return new SubScopeInstance<>(context.scopeRegistry(), typeName, typeArg);
        }
        if (Provider.class.isAssignableFrom(rawType)) {
            Class<Object> typeArg = (Class<Object>) extractTypeArgument(genericType);
            
            Annotation[] qualifiers = key.qualifiers().toArray(new Annotation[0]);
            ProducerCacheKey producerKey = ProducerCacheKey.of(typeArg, qualifiers);
            ProducerRegistry registry = context.producerRegistry();
            if (registry != null) {
                Optional<Provider<Object>> producer = registry.find(producerKey);
                if (producer.isPresent()) {
                    return producer.get();
                }
            }
            
            DotName typeName = DotName.createSimple(typeArg);
            return new SubScopeProvider<>(context.scopeRegistry(), typeName, typeArg);
        }
        if (Event.class.isAssignableFrom(rawType)) {
            Annotation[] eventQualifiers = filterQualifiers(key.qualifiers());
            return new EventInjectionPoint<>(context.scopeRegistry(), eventQualifiers);
        }
        if (Context.class.isAssignableFrom(rawType)) {
            if(DependentProviderStack.isInExtension()) {
                return context;
            } else {
                throw new ConstructionException("Injection of Context is disallowed outside of extensions");
            }
        }
        if (Optional.class.isAssignableFrom(rawType)) {
            Class<Object> typeArg = (Class<Object>) extractTypeArgument(genericType);
            
            Annotation[] qualifiers = key.qualifiers().toArray(new Annotation[0]);
            ProducerCacheKey producerKey = ProducerCacheKey.of(typeArg, qualifiers);
            ProducerRegistry registry = context.producerRegistry();
            
            if (registry != null) {
                Optional<Provider<Object>> producer = registry.find(producerKey);
                if (producer.isPresent()) {
                    try {
                        Object value = producer.get().get();
                        Object unwrapped = unwrapIfTargetAware(value, target, true);
                        return Optional.ofNullable(unwrapped);
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                }
            }
            
            DotName typeName = DotName.createSimple(typeArg);
            LimitedInstance instance = context.getInstance(typeName);
            
            if (instance.isUnsatisfied() || instance.isAmbiguous()) {
                return Optional.empty();
            }
            
            try {
                Object value = instance.get(context.loadClass(typeName));
                Object unwrapped = unwrapIfTargetAware(value, target, true);
                return Optional.ofNullable(unwrapped);
            } catch (Exception e) {
                return Optional.empty();
            }
        }
        boolean isOptional = isOptionalInjection(genericType, target);
        Annotation[] qualifiers = key.qualifiers().toArray(new Annotation[0]);
        ProducerCacheKey producerKey = ProducerCacheKey.of(genericType, qualifiers);
        ProducerRegistry registry = context.producerRegistry();
        if (registry != null) {
            Optional<Provider<Object>> producer = registry.find(producerKey);
            if (producer.isPresent()) {
                return unwrapIfTargetAware(producer.get().get(), target, isOptional);
            }
        }
        return unwrapIfTargetAware(
                context.getInstance(key.type()).get(context.loadClass(key.type())),
                target,
                isOptional
        );
    }

    private static Annotation[] filterQualifiers(List<Annotation> annotations) {
        return annotations.stream()
                .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                .toArray(Annotation[]::new);
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

    private static Object unwrapIfTargetAware(Object value, InjectionPoint target, boolean isOptional) {
        if (value instanceof TargetAwareProvider<?> tap && target != null) {
            return tap.get(target, isOptional);
        }
        return value;
    }
    
    private static boolean isOptionalInjection(Type genericType, InjectionPoint target) {
        if (genericType instanceof ParameterizedType pt) {
            if (pt.getRawType() == Optional.class) {
                return true;
            }
        }
        if (target != null && target.hasAnnotation(Nullable.class)) {
            return true;
        }
        return false;
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
