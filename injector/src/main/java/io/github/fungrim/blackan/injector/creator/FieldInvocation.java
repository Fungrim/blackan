package io.github.fungrim.blackan.injector.creator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.fungrim.blackan.common.cdi.InjectionTarget;
import io.github.fungrim.blackan.common.cdi.TargetType;
import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.lookup.RecursionKey;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FieldInvocation {

    private final Context context;
    private final Object object;
    private final Field field;
    private final RecursionKey key;

    public static List<FieldInvocation> of(Context context, Object object) {
        List<FieldInvocation> fieldInvocations = new ArrayList<>();
        for (Field field : object.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                fieldInvocations.add(of(context, object, field));
            }
        }
        return fieldInvocations;
    }

    public static FieldInvocation of(Context context, Object object, Field field) {
        return new FieldInvocation(context, object, field, extractInjectionPoints(field));
    }

    private static RecursionKey extractInjectionPoints(Field field) {
        Class<?> parameterType = field.getType();
        Annotation[] annotations = field.getAnnotations();
        return RecursionKey.of(parameterType, annotations);
    }

    public void set() {
        try {
            field.setAccessible(true);
            InjectionTarget target = new InjectionTarget(
                    field.getDeclaringClass(),
                    TargetType.FIELD,
                    field.getName(),
                    Arrays.stream(field.getAnnotations()).toList()
            );
            field.set(object, InvocationUtil.resolveInjectionPoint(context, key, field.getGenericType(), target));
            field.setAccessible(false);
        } catch (Exception e) {
            throw new ConstructionException("Failed to invoke method " + field.getName(), e);
        }
    }
}
