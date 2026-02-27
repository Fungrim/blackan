package io.github.fungrim.blackan.injector.util.stubs;

import io.github.fungrim.blackan.injector.Context;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class NonExtensionWithContextBean {

    @Inject
    public Context context;
}
