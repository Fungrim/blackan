package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

@ApplicationScoped
public class CircularProviderA {

    @Inject
    public Provider<CircularProviderB> providerB;

    public String value() {
        return "A";
    }
}
