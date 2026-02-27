package io.github.fungrim.blackan.injector.producer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.github.fungrim.blackan.common.cdi.TargetAwareProvider;
import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.Scope;
import io.github.fungrim.blackan.injector.creator.SingletonProvider;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Provider;

public class ProducerRegistry {

    private final Map<ProducerKey, Provider<?>> producers = new HashMap<>();

    public void scan(Context context, Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Produces.class)) {
                register(context, clazz, method);
            }
        }
    }

    private void register(Context context, Class<?> declaringClass, Method method) {
        Type returnType = method.getGenericReturnType();
        Annotation[] annotations = method.getAnnotations();
        ProducerKey key = ProducerKey.of(unwrapTargetAwareType(returnType), annotations);
        Provider<?> provider = new ProducerMethodProvider<>(context, declaringClass, method);
        Scope scope = Scope.of(method).orElse(Scope.DEPENDENT);
        if (scope != Scope.DEPENDENT) {
            provider = new SingletonProvider<>(provider);
        }
        producers.put(key, provider);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<Provider<T>> find(ProducerKey key) {
        return Optional.ofNullable((Provider<T>) producers.get(key));
    }

    public boolean isEmpty() {
        return producers.isEmpty();
    }

    private static Type unwrapTargetAwareType(Type type) {
        if (type instanceof ParameterizedType pt && pt.getRawType() == TargetAwareProvider.class) {
            return pt.getActualTypeArguments()[0];
        }
        return type;
    }
}
