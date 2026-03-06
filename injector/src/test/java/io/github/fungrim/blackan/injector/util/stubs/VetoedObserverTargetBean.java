package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class VetoedObserverTargetBean {

    public void onStringEvent(@Observes String event) {
        ContainerInitEventTracker.record("vetoedObserverFired");
    }
}
