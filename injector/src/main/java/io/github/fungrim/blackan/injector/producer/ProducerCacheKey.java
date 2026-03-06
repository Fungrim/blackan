package io.github.fungrim.blackan.injector.producer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.util.Nonbinding;

public record ProducerCacheKey(
    Class<?> rawType,
    List<Type> typeArguments,
    List<String> qualifierNames) {

    public static ProducerCacheKey of(Type returnType, Annotation[] annotations) {
        Class<?> raw;
        List<Type> typeArgs;
        if (returnType instanceof ParameterizedType pt) {
            raw = (Class<?>) pt.getRawType();
            typeArgs = List.of(pt.getActualTypeArguments());
        } else if (returnType instanceof Class<?> c) {
            raw = c;
            typeArgs = List.of();
        } else {
            throw new IllegalArgumentException("Unsupported return type: " + returnType);
        }
        List<String> qualifiers = Arrays.stream(annotations)
                .filter(a -> a.annotationType().isAnnotationPresent(jakarta.inject.Qualifier.class))
                .map(ProducerCacheKey::qualifierIdentity)
                .sorted()
                .toList();
        return new ProducerCacheKey(raw, typeArgs, qualifiers);
    }

    private static String qualifierIdentity(Annotation annotation) {
        Method[] methods = annotation.annotationType().getDeclaredMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName));
        StringBuilder sb = new StringBuilder("@").append(annotation.annotationType().getName()).append("(");
        boolean first = true;
        for (Method m : methods) {
            if (m.isAnnotationPresent(Nonbinding.class)) {
                continue;
            }
            if (!first) sb.append(", ");
            try {
                sb.append(m.getName()).append("=").append(m.invoke(annotation));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to read qualifier attribute: " + m.getName(), e);
            }
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProducerCacheKey other)) return false;
        return rawType.equals(other.rawType)
                && typeArguments.equals(other.typeArguments)
                && qualifierNames.equals(other.qualifierNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawType, typeArguments, qualifierNames);
    }
}
