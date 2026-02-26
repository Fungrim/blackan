package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CircularConstructorB {

    public final CircularConstructorA a;

    @Inject
    public CircularConstructorB(CircularConstructorA a) {
        this.a = a;
    }
}
