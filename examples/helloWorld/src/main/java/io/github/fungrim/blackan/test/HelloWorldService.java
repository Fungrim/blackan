package io.github.fungrim.blackan.test;

import org.slf4j.Logger;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Stage;
import io.github.fungrim.blackan.common.cdi.RuntimeStartEvent;
import io.github.fungrim.blackan.common.cdi.RuntimeStopEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@Priority(100)
@BootStage(Stage.APPLICATION)
public class HelloWorldService {

    @Inject
    Logger logger;

    public void start(@Observes RuntimeStartEvent event) {
        logger.info("Hello World Service started");
    }

    public void stop(@Observes RuntimeStopEvent event) {
        logger.info("Hello World Service stopped");
    }

    @PostConstruct
    public void init() {
        logger.info("Hello World Service initialized");
    }

    @PreDestroy
    public void destroy() {
        logger.info("Hello World Service destroyed");
    }
}
