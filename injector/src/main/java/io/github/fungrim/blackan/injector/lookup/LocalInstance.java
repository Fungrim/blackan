package io.github.fungrim.blackan.injector.lookup;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.stream.StreamSupport;

import org.jboss.jandex.ClassInfo;

import io.github.fungrim.blackan.injector.creator.ProviderFactory;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

class LocalInstance<T> implements Instance<T> {

    private final RecursiveInstance delegate;
    private final ProviderFactory providerFactory;
    private final Class<T> type;

    LocalInstance(RecursiveInstance delegate, ProviderFactory providerFactory, Class<T> type) {
        this.delegate = delegate;
        this.providerFactory = providerFactory;
        this.type = type;
    }

    @Override
    public Iterator<T> iterator() {
        return StreamSupport.stream(delegate.candidates().spliterator(), false)
                .map(c -> providerFactory.create(c, type).get())
                .iterator();
    }

    @Override
    public T get() {
        return delegate.get(type);
    }

    @Override
    public Instance<T> select(Annotation... qualifiers) {
        return delegate.select(qualifiers).toInstance(type);
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        return delegate.selectSubtype(subtype, qualifiers).toInstance(subtype);
    }

    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        Class<U> rawType = subtype.getRawType();
        return delegate.selectSubtype(rawType, qualifiers).toInstance(rawType);
    }

    @Override
    public boolean isUnsatisfied() {
        return delegate.isUnsatisfied();
    }

    @Override
    public boolean isAmbiguous() {
        return delegate.isAmbiguous();
    }

    @Override
    public void destroy(T instance) {
        // no-op: use Context.destroy() for full lifecycle + cache clearing
    }

    @Override
    public Handle<T> getHandle() {
        ClassInfo classInfo = delegate.getCandidate();
        return new LocalHandle<>(() -> providerFactory.create(classInfo, type).get());
    }

    @Override
    public Iterable<? extends Handle<T>> handles() {
        return () -> delegate.candidates().stream()
                .<Handle<T>>map(c -> new LocalHandle<>(() -> providerFactory.create(c, type).get()))
                .iterator();
    }

}
