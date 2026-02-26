package io.github.fungrim.blackan.injector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.injector.creator.ConstructionException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Singleton;

public enum Scope {

    APPLICATION(1),
    SESSION(2),
    REQUEST(3),
    DEPENDENT(-1),
    SINGLETON(-1);

    public static Optional<Scope> from(Annotation annotation) {
        return from(annotation.annotationType());
    }
    
    public static Optional<Scope> of(ClassInfo clazz) {
        List<Scope> scopes = clazz.annotations()
            .stream()
            .map(Scope::from)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
        if(scopes.isEmpty()) {
            return Optional.empty();
        } else if(scopes.size() > 1) {
            throw new ConstructionException("Multiple scopes found for: " + clazz.name());
        } else {
            return Optional.of(scopes.get(0));
        }
    }

    public static Optional<Scope> from(AnnotationInstance annotation) {
        if(annotation.name().equals(DotName.createSimple(ApplicationScoped.class))) {
            return Optional.of(APPLICATION);
        } else if(annotation.name().equals(DotName.createSimple(SessionScoped.class))) {
            return Optional.of(SESSION);
        } else if(annotation.name().equals(DotName.createSimple(RequestScoped.class))) {
            return Optional.of(REQUEST);
        } else if(annotation.name().equals(DotName.createSimple(Dependent.class))) {
            return Optional.of(DEPENDENT);
        } else if(annotation.name().equals(DotName.createSimple(Singleton.class))) {
            return Optional.of(SINGLETON);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<Scope> of(Class<?> clazz) {
        return from(findScope(clazz.getAnnotations(), clazz.getName()).orElse(Dependent.Literal.INSTANCE));
    }

    public static Optional<Scope> of(Field field) {
        Optional<Annotation> opt = findScope(field.getAnnotations(), field.getName());
        if(opt.isPresent()) {
            return from(opt.get());
        } else {
            return Optional.empty();
        }
    }

    public static Optional<Scope> of(Method method) {
        Optional<Annotation> opt = findScope(method.getAnnotations(), method.getName());
        if(opt.isPresent()) {
            return from(opt.get());
        } else {
            return Optional.empty();
        }
    }

    public static Optional<Scope> from(Class<? extends Annotation> annotation) {
        if(annotation.equals(ApplicationScoped.class)) {
            return Optional.of(APPLICATION);
        } else if(annotation.equals(SessionScoped.class)) {
            return Optional.of(SESSION);
        } else if(annotation.equals(RequestScoped.class)) {
            return Optional.of(REQUEST);
        } else if(annotation.equals(Dependent.class)) {
            return Optional.of(DEPENDENT);
        } else if(annotation.equals(Singleton.class)) {
            return Optional.of(SINGLETON);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Annotation> findScope(Annotation[] annotations, String name) {
        List<Annotation> scopes = List.of(annotations).stream()
                .filter(a -> a.annotationType().isAnnotationPresent(jakarta.inject.Scope.class)
                    || a.annotationType().isAnnotationPresent(NormalScope.class))
                .toList();
        if(scopes.isEmpty()) {
            return Optional.empty();
        } else if(scopes.size() > 1) {
            throw new ConstructionException("Multiple scopes found for: " + name);
        } else {
            return Optional.of(scopes.get(0));
        }
    }

    private final int priority;

    Scope(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }

    public boolean isPseudoScope() {
        return priority == -1;
    }

    public boolean isNormalScope() {
        return priority > 0;
    }

    public boolean isCachedInScope() {
        return this != DEPENDENT;
    }

    public boolean shouldYield(Scope other) {
        return priority() < other.priority();
    }
}
