package io.github.fungrim.blackan.extension.smallrye.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "server")
public interface ServerConfig {

    String host();

    int port();
}
