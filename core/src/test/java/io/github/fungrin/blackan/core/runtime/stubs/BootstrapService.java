package io.github.fungrin.blackan.core.runtime.stubs;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Service;
import io.github.fungrim.blackan.common.api.Stage;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@BootStage(Stage.BOOTSTRAP)
public class BootstrapService implements Service {

    @Override
    public void init() {
        LifecycleLog.record("bootstrap:init");
    }

    @Override
    public void destroy() {
        LifecycleLog.record("bootstrap:destroy");
    }

    @Override
    public void start() {
        LifecycleLog.record("bootstrap:start");
    }

    @Override
    public void stop() {
        LifecycleLog.record("bootstrap:stop");
    }
}
