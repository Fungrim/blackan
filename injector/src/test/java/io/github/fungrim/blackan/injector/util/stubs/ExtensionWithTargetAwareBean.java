package io.github.fungrim.blackan.injector.util.stubs;

import io.github.fungrim.blackan.common.api.Extension;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Extension
@ApplicationScoped
public class ExtensionWithTargetAwareBean {

    @Inject
    @Named("target-aware-greeting")
    public String greeting;
}
