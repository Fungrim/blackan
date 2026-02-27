package io.github.fungrim.blackan.injector.util.stubs;

import io.github.fungrim.blackan.common.api.Extension;
import io.github.fungrim.blackan.common.cdi.TargetAwareProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

@Extension
@ApplicationScoped
public class TargetAwareProducer {

    @Produces
    @Named("target-aware-greeting")
    public TargetAwareProvider<String> greetingProvider() {
        return target -> "hello from " + target.parentClass().getSimpleName() + "." + target.targetName();
    }
}
