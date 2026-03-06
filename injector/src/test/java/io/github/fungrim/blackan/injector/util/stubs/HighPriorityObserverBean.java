package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class HighPriorityObserverBean {

    public void onStringEvent(@Observes @Priority(100) String event) {
        EventOrderTracker.record(HighPriorityObserverBean.class);
    }
}
