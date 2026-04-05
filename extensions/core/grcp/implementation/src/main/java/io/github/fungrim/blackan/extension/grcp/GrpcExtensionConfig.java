package io.github.fungrim.blackan.extension.grcp;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "blackan.grpc.server")
public interface GrpcExtensionConfig {

    @WithName("port")
    @WithDefault("50051")
    int port();

    @WithName("bind-address")
    @WithDefault("0.0.0.0")
    String bindAddress();

}
