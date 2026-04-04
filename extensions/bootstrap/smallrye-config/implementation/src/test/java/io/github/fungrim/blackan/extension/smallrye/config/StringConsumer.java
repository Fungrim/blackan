package io.github.fungrim.blackan.extension.smallrye.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class StringConsumer {

    @Inject
    @ConfigProperty(name = "test.string")
    private String value;

    String value() {
        return value;
    }
}
