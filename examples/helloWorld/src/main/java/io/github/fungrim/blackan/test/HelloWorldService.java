package io.github.fungrim.blackan.test;

import org.slf4j.Logger;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Service;
import io.github.fungrim.blackan.common.api.Stage;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;

@Priority(100)
@BootStage(Stage.APPLICATION)
public class HelloWorldService implements Service {

    @Inject
    Logger logger;

    @Override
    public void start() {
        logger.info("Hello World Service started");
    }

    @Override
    public void stop() {
        logger.info("Hello World Service stopped");
    }

    @Override
    public void init() {
        logger.info("Hello World Service initialized");
    }

    @Override
    public void destroy() {
        logger.info("Hello World Service destroyed");
    }
}
