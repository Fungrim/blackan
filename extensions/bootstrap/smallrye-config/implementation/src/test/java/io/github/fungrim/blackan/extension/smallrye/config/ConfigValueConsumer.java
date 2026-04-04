package io.github.fungrim.blackan.extension.smallrye.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.config.ConfigValue;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ConfigValueConsumer {

    @Inject
    @ConfigProperty(name = "test.string")
    private ConfigValue value;

    ConfigValue value() {
        return value;
    }
}
