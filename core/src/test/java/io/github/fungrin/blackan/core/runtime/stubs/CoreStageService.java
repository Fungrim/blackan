package io.github.fungrin.blackan.core.runtime.stubs;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Service;
import io.github.fungrim.blackan.common.api.Stage;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@BootStage(Stage.CORE)
public class CoreStageService implements Service {

    @Override
    public void init() {
        LifecycleLog.record("core:init");
    }

    @Override
    public void destroy() {
        LifecycleLog.record("core:destroy");
    }

    @Override
    public void start() {
        LifecycleLog.record("core:start");
    }

    @Override
    public void stop() {
        LifecycleLog.record("core:stop");
    }
}
