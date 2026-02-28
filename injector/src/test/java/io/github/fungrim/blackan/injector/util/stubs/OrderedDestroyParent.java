package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderedDestroyParent {

    @Inject
    public OrderedDestroyDependent dependent;

    @PreDestroy
    public void destroy() {
        LifecycleOrderTracker.record(OrderedDestroyParent.class, "preDestroy");
    }
}
