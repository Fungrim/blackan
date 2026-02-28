package io.github.fungrim.blackan.injector.creator;

import java.lang.annotation.Annotation;
import java.util.Iterator;

import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.context.ScopeRegistry;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SubScopeInstance<T> implements Instance<T> {

    private final ScopeRegistry scope;
    private final DotName type;
    private final Class<T> clazz;

    @Override
    public T get() {
        return current().getInstance(type).get(clazz);
    }

    private Context current() {
        return scope.current().orElseThrow(() -> new IllegalStateException("No current context found"));
    }

    @Override
    public Instance<T> select(Annotation... qualifiers) {
        throw new UnsupportedOperationException("select not supported on SubscopeInstance");
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException("select not supported on SubscopeInstance");
    }

    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException("select not supported on SubscopeInstance");
    }

    @Override
    public boolean isUnsatisfied() {
        return current().getInstance(type).isUnsatisfied();
    }

    @Override
    public boolean isAmbiguous() {
        return current().getInstance(type).isAmbiguous();
    }

    @Override
    public void destroy(T instance) {
        throw new UnsupportedOperationException("destroy not supported on SubscopeInstance");
    }

    @Override
    public Handle<T> getHandle() {
        throw new UnsupportedOperationException("getHandle not supported on SubscopeInstance");
    }

    @Override
    public Iterable<? extends Handle<T>> handles() {
        throw new UnsupportedOperationException("handles not supported on SubscopeInstance");
    }

    @Override
    public Iterator<T> iterator() {
        return current().getInstance(type).toInstance(clazz).iterator();
    }
}
