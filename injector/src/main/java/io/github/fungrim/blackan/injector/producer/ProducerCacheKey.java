package io.github.fungrim.blackan.injector.producer;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
        return annotation.toString();
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
