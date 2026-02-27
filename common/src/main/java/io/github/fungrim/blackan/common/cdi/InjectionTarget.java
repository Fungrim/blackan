package io.github.fungrim.blackan.common.cdi;

import java.lang.annotation.Annotation;
import java.util.List;

public record InjectionTarget(
    Class<?> parentClass,
    TargetType targetType,
    String targetName,
    List<Annotation> qualifiers
) {

}
