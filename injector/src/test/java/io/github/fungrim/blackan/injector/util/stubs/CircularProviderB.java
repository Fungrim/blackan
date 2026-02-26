package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

@ApplicationScoped
public class CircularProviderB {

    @Inject
    public Provider<CircularProviderA> providerA;

    public String value() {
        return "B";
    }
}
