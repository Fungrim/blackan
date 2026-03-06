package io.github.fungrim.blackan.injector.lookup;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;

import io.github.fungrim.blackan.injector.context.ScopeRegistry;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.util.TypeLiteral;

public class EventInjectionPoint<T> implements Event<T> {

    private final ScopeRegistry scopeRegistry;
    private final Annotation[] qualifiers;

    public EventInjectionPoint(ScopeRegistry scopeRegistry, Annotation[] qualifiers) {
        this.scopeRegistry = scopeRegistry;
        this.qualifiers = qualifiers.clone();
    }

    @Override
    public void fire(T event) {
        currentContext().fire(event, qualifiers);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends T> CompletionStage<U> fireAsync(U event) {
        return (CompletionStage<U>) currentContext().fireAsync(event, qualifiers);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends T> CompletionStage<U> fireAsync(U event, NotificationOptions options) {
        return (CompletionStage<U>) currentContext().fireAsync(event, options, qualifiers);
    }

    @Override
    public Event<T> select(Annotation... additionalQualifiers) {
        return new EventInjectionPoint<>(scopeRegistry, mergeQualifiers(additionalQualifiers));
    }

    @Override
    public <U extends T> Event<U> select(Class<U> subtype, Annotation... additionalQualifiers) {
        return new EventInjectionPoint<>(scopeRegistry, mergeQualifiers(additionalQualifiers));
    }

    @Override
    public <U extends T> Event<U> select(TypeLiteral<U> subtype, Annotation... additionalQualifiers) {
        return new EventInjectionPoint<>(scopeRegistry, mergeQualifiers(additionalQualifiers));
    }

    private io.github.fungrim.blackan.injector.Context currentContext() {
        return scopeRegistry.current()
                .orElseThrow(() -> new IllegalStateException("No current context"));
    }

    private Annotation[] mergeQualifiers(Annotation[] additional) {
        if (additional == null || additional.length == 0) {
            return qualifiers.clone();
        }
        Annotation[] merged = Arrays.copyOf(qualifiers, qualifiers.length + additional.length);
        System.arraycopy(additional, 0, merged, qualifiers.length, additional.length);
        return merged;
    }
}
