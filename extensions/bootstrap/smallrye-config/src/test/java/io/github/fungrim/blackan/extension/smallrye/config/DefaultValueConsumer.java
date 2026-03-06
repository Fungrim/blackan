package io.github.fungrim.blackan.extension.smallrye.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class DefaultValueConsumer {

    @Inject
    @ConfigProperty(name = "test.absent.property", defaultValue = "default-value")
    private String value;

    String value() {
        return value;
    }
}
