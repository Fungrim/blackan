package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;

@Priority(1)
@ApplicationScoped
public class SecondLifecycleBean {

    @Initialized(ApplicationScoped.class)
    public void onInitialized() {
        LifecycleOrderTracker.record(SecondLifecycleBean.class, "initialized");
    }
}
