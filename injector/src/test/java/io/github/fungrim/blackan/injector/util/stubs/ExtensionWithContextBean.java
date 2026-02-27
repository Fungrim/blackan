package io.github.fungrim.blackan.injector.util.stubs;

import io.github.fungrim.blackan.common.api.Extension;
import io.github.fungrim.blackan.injector.Context;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Extension
@ApplicationScoped
public class ExtensionWithContextBean {

    @Inject
    public Context context;
}
