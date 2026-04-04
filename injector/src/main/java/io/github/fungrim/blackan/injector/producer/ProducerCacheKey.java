package io.github.fungrim.blackan.injector.producer;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Named;

public record ProducerCacheKey(
    Class<?> rawType,
    List<Type> typeArguments,
    List<QualifierIdentity> qualifiers) {

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
        List<QualifierIdentity> qualifiers = normalizeQualifiers(annotations);
        return new ProducerCacheKey(raw, typeArgs, qualifiers);
    }

    private static List<QualifierIdentity> normalizeQualifiers(Annotation[] annotations) {
        List<Annotation> rawQualifiers = Arrays.stream(annotations)
                .filter(a -> a.annotationType().isAnnotationPresent(jakarta.inject.Qualifier.class))
                .toList();
        
        boolean hasCustomQualifier = rawQualifiers.stream()
                .anyMatch(a -> !isPredefinedQualifier(a));
        boolean hasAny = rawQualifiers.stream()
                .anyMatch(a -> a.annotationType() == Any.class);
        boolean hasDefault = rawQualifiers.stream()
                .anyMatch(a -> a.annotationType() == Default.class);
        
        List<QualifierIdentity> result = new java.util.ArrayList<>(rawQualifiers.stream()
                .map(QualifierIdentity::of)
                .toList());
        
        if (!hasAny && !hasDefault && !hasCustomQualifier) {
            result.add(QualifierIdentity.of(Default.Literal.INSTANCE));
        }
        
        return result;
    }

    private static boolean isPredefinedQualifier(Annotation a) {
        Class<?> type = a.annotationType();
        return type == Default.class 
            || type == Any.class 
            || type == Named.class;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(rawType.getSimpleName());
        if (typeArguments != null && !typeArguments.isEmpty()) {
            sb.append(" <");
            sb.append(typeArguments.stream().map(Type::getTypeName).collect(java.util.stream.Collectors.joining(", ")));
            sb.append(">");
        }
        if (qualifiers != null && !qualifiers.isEmpty()) {
            sb.append(" | ");
            sb.append(qualifiers.stream().map(QualifierIdentity::toString).collect(java.util.stream.Collectors.joining(", ")));
        }
        return sb.toString();
    }
}
