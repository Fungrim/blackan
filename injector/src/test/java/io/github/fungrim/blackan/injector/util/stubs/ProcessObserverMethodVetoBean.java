package io.github.fungrim.blackan.injector.util.stubs;

import io.github.fungrim.blackan.common.cdi.ProcessObserverMethod;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class ProcessObserverMethodVetoBean {

    public void onProcessObserverMethod(@Observes ProcessObserverMethod event) {
        if (event.observerMethod().method().declaringClass().name().toString()
                .equals(VetoedObserverTargetBean.class.getName())) {
            event.veto();
        }
    }
}
