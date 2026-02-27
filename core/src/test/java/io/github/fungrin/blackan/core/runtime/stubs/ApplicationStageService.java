package io.github.fungrin.blackan.core.runtime.stubs;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Service;
import io.github.fungrim.blackan.common.api.Stage;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@BootStage(Stage.APPLICATION)
public class ApplicationStageService implements Service {

    @Override
    public void init() {
        LifecycleLog.record("application:init");
    }

    @Override
    public void destroy() {
        LifecycleLog.record("application:destroy");
    }

    @Override
    public void start() {
        LifecycleLog.record("application:start");
    }

    @Override
    public void stop() {
        LifecycleLog.record("application:stop");
    }
}
