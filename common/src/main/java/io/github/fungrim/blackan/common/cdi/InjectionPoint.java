package io.github.fungrim.blackan.common.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

public record InjectionPoint(
    Class<?> parentClass,
    InjectionPointType injectPointType,
    String targetName,
    Class<?> targetType,
    List<Type> targetTypeArguments,
    List<Annotation> qualifiers
) {

    public boolean hasAnnotation(Class<? extends Annotation> annotation) {
        return getAnnotation(annotation).isPresent();
    }

    public Optional<Annotation> getAnnotation(Class<? extends Annotation> annotation) {
        return qualifiers.stream().filter(q -> q.annotationType().equals(annotation)).findFirst();
    }

    @Override
    public String toString() {
        return "InjectionPoint {parentClass=" + parentClass + ", injectPointType=" + injectPointType + ", targetName=" + targetName + ", targetType=" + targetType + ", targetTypeArguments=" + targetTypeArguments + ", qualifiers=" + qualifiers + "}";
    }

    public static InjectionPoint of(Constructor<?> ctor, Parameter parameter) {
        return new InjectionPoint(
            ctor.getDeclaringClass(),
            InjectionPointType.CONSTRUCTOR,
            parameter.getName(),
            parameter.getType(),
            extractTypeArguments(parameter),
            List.of(parameter.getAnnotations())
        );
    }

    private static List<Type> extractTypeArguments(Parameter parameter) {
        if (parameter.getParameterizedType() instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            return List.of(args);
        }
        return List.of();
    }

    public static InjectionPoint of(Field field) {
        return new InjectionPoint(
            field.getDeclaringClass(),
            InjectionPointType.FIELD,
            field.getName(),
            field.getType(),
            extractTypeArguments(field),
            List.of(field.getAnnotations())
        );
    }

    private static List<Type> extractTypeArguments(Field field) {
        if (field.getGenericType() instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            return List.of(args);
        }
        return List.of();
    }

    public static InjectionPoint of(Method m, Parameter parameter) {
        return new InjectionPoint(
            m.getDeclaringClass(),
            InjectionPointType.METHOD,
            parameter.getName(),
            parameter.getType(),
            extractTypeArguments(parameter),
            List.of(parameter.getAnnotations())
        );
    }
}
