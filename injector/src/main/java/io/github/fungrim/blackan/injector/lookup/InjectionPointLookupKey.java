package io.github.fungrim.blackan.injector.lookup;

import java.lang.annotation.Annotation;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.common.util.Arguments;
import jakarta.validation.constraints.NotNull;

public record InjectionPointLookupKey(
    @NotNull List<Annotation> qualifiers,
    @NotNull DotName type) {

    public static InjectionPointLookupKey of(ClassInfo type, Annotation... qualifiers) {
        Arguments.notNull(type, "Type");
        Arguments.notNull(qualifiers, "Qualifiers");
        return new InjectionPointLookupKey(List.of(qualifiers), type.name());
    }

    public static InjectionPointLookupKey of(DotName type, Annotation... qualifiers) {
        Arguments.notNull(type, "Type");
        Arguments.notNull(qualifiers, "Qualifiers");
        return new InjectionPointLookupKey(List.of(qualifiers), type);
    }

    public static InjectionPointLookupKey of(Class<?> type, Annotation... qualifiers) {
        Arguments.notNull(type, "Type");
        Arguments.notNull(qualifiers, "Qualifiers");
        return new InjectionPointLookupKey(List.of(qualifiers), DotName.createSimple(type));
    }

    public static InjectionPointLookupKey of(ClassInfo type) {
        Arguments.notNull(type, "Type");
        return new InjectionPointLookupKey(List.of(), type.name());
    }

    public static InjectionPointLookupKey of(Class<?> type) {
        Arguments.notNull(type, "Type");
        return new InjectionPointLookupKey(List.of(), DotName.createSimple(type));
    }
}
