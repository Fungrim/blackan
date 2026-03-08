package io.github.fungrim.blackan.extension.jetty;

import org.slf4j.Logger;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Extension;
import io.github.fungrim.blackan.common.api.Stage;
import io.github.fungrim.blackan.common.cdi.RuntimeStartEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@Extension
@Priority(100) 
@BootStage(Stage.CORE)
public class JettyExtension {

    @Inject
    ServerConfig serverConfig;

    @Inject
    Logger log;

    @PostConstruct
    public void init() {
        log.info("JettyExtension initialized with: bindAddress {}; port {}; and hostName {}", serverConfig.bindAddress(), serverConfig.port(), serverConfig.hostName());
    }

    public void start(@Observes RuntimeStartEvent event) {
        log.info("JettyExtension starting");
    }
}
