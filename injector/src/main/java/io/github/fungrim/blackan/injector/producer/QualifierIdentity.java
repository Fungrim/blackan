package io.github.fungrim.blackan.injector.producer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.common.util.Tuple;
import jakarta.enterprise.util.Nonbinding;

public record QualifierIdentity(DotName qualifier, List<Tuple<String>> values) {

    public static QualifierIdentity of(Annotation annotation) {
        return new QualifierIdentity(DotName.createSimple(annotation.annotationType().getName()), toValues(annotation));
    }
    
    private static List<Tuple<String>> toValues(Annotation annotation) {
        Method[] methods = annotation.annotationType().getDeclaredMethods();
        List<Tuple<String>> values = new ArrayList<>(methods.length);
        Arrays.sort(methods, Comparator.comparing(Method::getName));
        for (Method m : methods) {
            if (m.isAnnotationPresent(Nonbinding.class)) {
                continue;
            }
            try {
                var name = m.getName();
                var value = m.invoke(annotation);
                values.add(new Tuple<>(name, String.valueOf(value)));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to read qualifier attribute: " + m.getName(), e);
            }
        }
        return values;
    }
    
    @Override
    public String toString() {
        if(values == null || values.isEmpty()) {
            return "@" + qualifier.withoutPackagePrefix();
        } else {
            return "@" + qualifier.withoutPackagePrefix() + " (" + values.stream().map(t -> t.first() + "=" + t.second()).collect(java.util.stream.Collectors.joining(", ")) + ")";
        }
    }
}
