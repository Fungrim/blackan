package io.github.fungrim.blackan.extension.smallrye.config;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ServerConfigConsumer {

    @Inject
    private ServerConfig config;

    ServerConfig config() {
        return config;
    }
}
