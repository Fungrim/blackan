package io.github.fungrim.blackan.injector.creator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.github.fungrim.blackan.common.cdi.InjectionPoint;
import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.lookup.InjectionPointLookupKey;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FieldInvocation {

    private final Context context;
    private final Object object;
    private final Field field;
    private final InjectionPointLookupKey key;

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

    private static InjectionPointLookupKey extractInjectionPoints(Field field) {
        Class<?> parameterType = field.getType();
        Annotation[] annotations = field.getAnnotations();
        return InjectionPointLookupKey.of(parameterType, annotations);
    }

    public void set() {
        try {
            field.setAccessible(true);
            InjectionPoint target = InjectionPoint.of(field);
            field.set(object, InjectionPointResolver.resolveInjectionPoint(context, key, field.getGenericType(), target));
            field.setAccessible(false);
        } catch (Exception e) {
            throw new ConstructionException("Failed to set field " + field.getName() + (Objects.isNull(object) ? "" : " on " + object.getClass().getName()), e);
        }
    }
}
