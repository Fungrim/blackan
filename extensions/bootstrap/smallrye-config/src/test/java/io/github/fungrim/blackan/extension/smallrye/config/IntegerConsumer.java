package io.github.fungrim.blackan.extension.smallrye.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class IntegerConsumer {

    @Inject
    @ConfigProperty(name = "test.integer")
    private Integer value;

    Integer value() {
        return value;
    }
}
