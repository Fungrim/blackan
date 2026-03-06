package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class LowPriorityObserverBean {

    public void onStringEvent(@Observes @Priority(2000) String event) {
        EventOrderTracker.record(LowPriorityObserverBean.class);
    }
}
