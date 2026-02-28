package io.github.fungrim.blackan.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Stage;
import io.github.fungrim.blackan.common.cdi.RuntimeStartEvent;
import io.github.fungrim.blackan.common.cdi.RuntimeStopEvent;
import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.runtime.stubs.ApplicationStageService;
import io.github.fungrim.blackan.runtime.stubs.BootstrapService;
import io.github.fungrim.blackan.runtime.stubs.CoreStageService;
import io.github.fungrim.blackan.runtime.stubs.LifecycleLog;
import io.github.fungrim.blackan.runtime.util.StageAndPriorityComparator;

class RuntimeControllerTest {

    private RuntimeController controller;
    private Context root;

    @BeforeEach
    void setup() throws IOException {
        LifecycleLog.reset();
        root = Context.builder()
                .withClasses(List.of(
                        BootStage.class,
                        Stage.class,
                        RuntimeStartEvent.class,
                        RuntimeStopEvent.class,
                        BootstrapService.class,
                        CoreStageService.class,
                        ApplicationStageService.class))
                .withEventOrdering(new StageAndPriorityComparator())
                .build();
        controller = new RuntimeController(root);
    }

    @Test
    void initCallsServicesInStageOrder() {
        root.get(BootstrapService.class);
        root.get(CoreStageService.class);
        root.get(ApplicationStageService.class);
        assertEquals(List.of(
                "bootstrap:init",
                "core:init",
                "application:init"), LifecycleLog.events());
    }

    @Test
    void startCallsServicesInStageOrder() {
        root.get(BootstrapService.class);
        root.get(CoreStageService.class);
        root.get(ApplicationStageService.class);
        controller.start();
        assertEquals(List.of(
                "bootstrap:init",
                "core:init",
                "application:init",    
                "bootstrap:start",
                "core:start",
                "application:start"), LifecycleLog.events());
    }

    @Test
    void stopCallsServicesInReverseStageOrder() {
        root.get(BootstrapService.class);
        root.get(CoreStageService.class);
        root.get(ApplicationStageService.class);
        controller.stop();
        assertEquals(List.of(
                "bootstrap:init",
                "core:init",
                "application:init",
                "application:stop",
                "core:stop",
                "bootstrap:stop"), LifecycleLog.events());
    }

    @Test
    void fullLifecycleExecutesInCorrectOrder() {
        root.get(BootstrapService.class);
        root.get(CoreStageService.class);
        root.get(ApplicationStageService.class);
        controller.start();
        controller.stop();
        root.destroy(ApplicationStageService.class);
        root.destroy(CoreStageService.class);
        root.destroy(BootstrapService.class);
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
