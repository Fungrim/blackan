package io.github.fungrim.blackan.injector.util.stubs;

import io.github.fungrim.blackan.common.cdi.BeforeShutdown;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class BeforeShutdownObserverBean {

    public void onBeforeShutdown(@Observes BeforeShutdown event) {
        ContainerInitEventTracker.record("beforeShutdown");
    }

    public void onBeforeDestroyed(@Observes @BeforeDestroyed(ApplicationScoped.class) Object event) {
        ContainerInitEventTracker.record("beforeDestroyed");
    }
}
