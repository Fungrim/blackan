package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CircularConstructorA {

    public final CircularConstructorB b;

    @Inject
    public CircularConstructorA(CircularConstructorB b) {
        this.b = b;
    }
}
