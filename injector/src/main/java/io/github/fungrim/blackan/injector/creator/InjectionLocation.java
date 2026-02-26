package io.github.fungrim.blackan.injector.creator;

import java.lang.annotation.Annotation;
import java.util.Optional;

public record InjectionLocation<T>(
    Class<T> parentClass,
    Optional<Object> parentInstance,
    Annotation[] qualifiers,
    InjectionLocationInvoker invoker
) {

}
