package io.github.fungrim.blackan.runtime.stubs;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Stage;
import io.github.fungrim.blackan.common.cdi.RuntimeStartEvent;
import io.github.fungrim.blackan.common.cdi.RuntimeStopEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
@BootStage(Stage.CORE)
public class CoreStageService {

    @PostConstruct
    public void init() {
        LifecycleLog.record("core:init");
    }

    @PreDestroy
    public void destroy() {
        LifecycleLog.record("core:destroy");
    }

    public void start(@Observes RuntimeStartEvent event) {
        LifecycleLog.record("core:start");
    }

    public void stop(@Observes RuntimeStopEvent event) {
        LifecycleLog.record("core:stop");
    }
}
