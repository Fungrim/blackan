package io.github.fungrim.blackan.extension.jetty;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "blackan.http.server")
public interface ServerConfig {

    @WithName("bind-address")
    @WithDefault("0.0.0.0")
    String bindAddress();

    @WithName("port")
    @WithDefault("8080")
    int port();
    
    Threading threading();

    public static interface Threading {

        @WithDefault("-1")
        int maxConcurrentTasks();

        @WithDefault("30000")
        int idleTimeout();
    
    }
}
