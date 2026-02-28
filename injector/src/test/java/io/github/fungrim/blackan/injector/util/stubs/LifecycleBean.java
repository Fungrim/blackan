package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class LifecycleBean {

    public void onInitialized(@Observes @Initialized(ApplicationScoped.class) Object event) {
        LifecycleOrderTracker.record(LifecycleBean.class, "initialized");
    }

    public void onBeforeDestroyed(@Observes @BeforeDestroyed(ApplicationScoped.class) Object event) {
        LifecycleOrderTracker.record(LifecycleBean.class, "beforeDestroyed");
    }

    public void onDestroyed(@Observes @Destroyed(ApplicationScoped.class) Object event) {
        LifecycleOrderTracker.record(LifecycleBean.class, "destroyed");
    }
}
