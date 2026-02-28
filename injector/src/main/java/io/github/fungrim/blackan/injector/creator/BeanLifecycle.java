package io.github.fungrim.blackan.injector.creator;

import java.lang.reflect.Method;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BeanLifecycle {

    public static void invokePostConstruct(Object instance) {
        findMethod(instance.getClass(), PostConstruct.class).ifPresent(m -> invoke(instance, m));
    }

    public static Optional<Method> findPreDestroy(Class<?> clazz) {
        return findMethod(clazz, PreDestroy.class);
    }

    private static Optional<Method> findMethod(Class<?> clazz, Class<? extends java.lang.annotation.Annotation> annotation) {
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotation)) {
                    return Optional.of(method);
                }
            }
        }
        return Optional.empty();
    }

    static void invoke(Object instance, Method method) {
        try {
            method.setAccessible(true);
            method.invoke(instance);
            method.setAccessible(false);
        } catch (Exception e) {
            throw new ConstructionException(
                    "Failed to invoke lifecycle method " + method.getDeclaringClass().getName() + "." + method.getName(), e);
        }
    }
}
