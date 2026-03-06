package io.github.fungrim.blackan.extension.smallrye.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class BooleanConsumer {

    @Inject
    @ConfigProperty(name = "test.boolean")
    private Boolean value;

    Boolean value() {
        return value;
    }
}
