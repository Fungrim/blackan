package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;

@ApplicationScoped
public class LifecycleBean {

    @Initialized(ApplicationScoped.class)
    public void onInitialized() {
        LifecycleOrderTracker.record(LifecycleBean.class, "initialized");
    }

    @BeforeDestroyed(ApplicationScoped.class)
    public void onBeforeDestroyed() {
        LifecycleOrderTracker.record(LifecycleBean.class, "beforeDestroyed");
    }

    @Destroyed(ApplicationScoped.class)
    public void onDestroyed() {
        LifecycleOrderTracker.record(LifecycleBean.class, "destroyed");
    }
}
