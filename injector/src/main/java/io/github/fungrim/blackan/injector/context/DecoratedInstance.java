package io.github.fungrim.blackan.injector.context;

public interface DecoratedInstance<T> {

    public T get();

    public void destroy();
    
}
