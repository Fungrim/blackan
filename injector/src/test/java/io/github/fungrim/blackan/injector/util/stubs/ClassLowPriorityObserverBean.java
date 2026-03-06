package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
@Priority(100)
public class ClassLowPriorityObserverBean {

    public void onStringEvent(@Observes String event) {
        EventOrderTracker.record(ClassLowPriorityObserverBean.class);
    }
}
