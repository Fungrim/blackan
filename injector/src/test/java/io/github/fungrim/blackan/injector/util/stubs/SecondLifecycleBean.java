package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;

@Priority(1)
@ApplicationScoped
public class SecondLifecycleBean {

    public void onInitialized(@Observes @Initialized(ApplicationScoped.class) Object event) {
        LifecycleOrderTracker.record(SecondLifecycleBean.class, "initialized");
    }
}
