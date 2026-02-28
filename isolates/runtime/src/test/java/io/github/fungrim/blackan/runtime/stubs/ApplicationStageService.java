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
@BootStage(Stage.APPLICATION)
public class ApplicationStageService {

    @PostConstruct
    public void init() {
        LifecycleLog.record("application:init");
    }

    @PreDestroy
    public void destroy() {
        LifecycleLog.record("application:destroy");
    }

    public void start(@Observes RuntimeStartEvent event) {
        LifecycleLog.record("application:start");
    }

    public void stop(@Observes RuntimeStopEvent event) {
        LifecycleLog.record("application:stop");
    }
}
