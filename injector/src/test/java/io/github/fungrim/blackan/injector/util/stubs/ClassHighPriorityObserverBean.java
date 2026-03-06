package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
@Priority(1)
public class ClassHighPriorityObserverBean {

    public void onStringEvent(@Observes String event) {
        EventOrderTracker.record(ClassHighPriorityObserverBean.class);
    }
}
