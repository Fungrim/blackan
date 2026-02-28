package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;

@RequestScoped
public class RequestScopedLifecycleBean {

    public void onRequestInitialized(@Observes @Initialized(RequestScoped.class) Object event) {
        LifecycleOrderTracker.record(RequestScopedLifecycleBean.class, "initialized");
    }
}
