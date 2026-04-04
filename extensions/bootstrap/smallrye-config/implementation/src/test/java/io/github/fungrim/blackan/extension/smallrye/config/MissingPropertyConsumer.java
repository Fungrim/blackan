package io.github.fungrim.blackan.extension.smallrye.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class MissingPropertyConsumer {

    @Inject
    @ConfigProperty(name = "test.definitely.missing.property.xyz")
    private String value;

    String value() {
        return value;
    }
}
