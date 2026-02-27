package io.github.fungrin.blackan.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Initializable;
import io.github.fungrim.blackan.common.api.Service;
import io.github.fungrim.blackan.common.api.Stage;
import io.github.fungrim.blackan.common.api.Startable;
import io.github.fungrim.blackan.injector.context.RootContext;
import io.github.fungrin.blackan.core.runtime.stubs.ApplicationStageService;
import io.github.fungrin.blackan.core.runtime.stubs.BootstrapService;
import io.github.fungrin.blackan.core.runtime.stubs.CoreStageService;
import io.github.fungrin.blackan.core.runtime.stubs.LifecycleLog;

class RuntimeControllerTest {

    private RuntimeController controller;

    @BeforeEach
    void setup() throws IOException {
        LifecycleLog.reset();
        RootContext root = RootContext.builder()
                .withClasses(List.of(
                        Service.class,
                        Initializable.class,
                        Startable.class,
                        BootStage.class,
                        Stage.class,
                        BootstrapService.class,
                        CoreStageService.class,
                        ApplicationStageService.class))
                .build();
        controller = new RuntimeController(root);
    }

    @Test
    void initCallsServicesInStageOrder() {
        controller.init();
        assertEquals(List.of(
                "bootstrap:init",
                "core:init",
                "application:init"), LifecycleLog.events());
    }

    @Test
    void destroyCallsServicesInReverseStageOrder() {
        controller.destroy();
        assertEquals(List.of(
                "application:destroy",
                "core:destroy",
                "bootstrap:destroy"), LifecycleLog.events());
    }

    @Test
    void startCallsServicesInStageOrder() {
        controller.start();
        assertEquals(List.of(
                "bootstrap:start",
                "core:start",
                "application:start"), LifecycleLog.events());
    }

    @Test
    void stopCallsServicesInReverseStageOrder() {
        controller.stop();
        assertEquals(List.of(
                "application:stop",
                "core:stop",
                "bootstrap:stop"), LifecycleLog.events());
    }

    @Test
    void fullLifecycleExecutesInCorrectOrder() {
        controller.init();
        controller.start();
        controller.stop();
        controller.destroy();
        assertEquals(List.of(
                "bootstrap:init",
                "core:init",
                "application:init",
                "bootstrap:start",
                "core:start",
                "application:start",
                "application:stop",
                "core:stop",
                "bootstrap:stop",
                "application:destroy",
                "core:destroy",
                "bootstrap:destroy"), LifecycleLog.events());
    }
}
