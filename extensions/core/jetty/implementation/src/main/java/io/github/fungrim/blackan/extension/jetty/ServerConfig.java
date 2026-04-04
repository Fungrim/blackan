package io.github.fungrim.blackan.extension.jetty;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "blackan.http.server")
public interface ServerConfig {

    @WithDefault("0.0.0.0")
    String bindAddress();

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
