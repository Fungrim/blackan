package io.github.fungrim.blackan.injector.producer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.jandex.MethodInfo;

import io.github.fungrim.blackan.common.cdi.TargetAwareProvider;
import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.Scope;
import io.github.fungrim.blackan.injector.creator.SingletonProvider;
import io.github.fungrim.blackan.injector.util.Jandex;
import jakarta.inject.Provider;

public class ProducerRegistry {

    private final Map<ProducerCacheKey, Provider<?>> producers = new ConcurrentHashMap<>();

    public void register(Context context, MethodInfo methodInfo) {
        Method method = Jandex.toMethod(methodInfo, context.classLoader());
        register(context, method.getDeclaringClass(), method);
    }

    private void register(Context context, Class<?> declaringClass, Method method) {
        Type returnType = method.getGenericReturnType();
        Annotation[] annotations = method.getAnnotations();
        ProducerCacheKey key = ProducerCacheKey.of(unwrapTargetAwareType(returnType), annotations);
        Provider<?> provider = new ProducerMethodProvider<>(context, declaringClass, method);
        Scope scope = Scope.of(method).orElse(Scope.DEPENDENT);
        if (scope != Scope.DEPENDENT) {
            provider = new SingletonProvider<>(provider);
        }
        producers.put(key, provider);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<Provider<T>> find(ProducerCacheKey key) {
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
    
    public void close() {
        producers.clear();
    }
}
