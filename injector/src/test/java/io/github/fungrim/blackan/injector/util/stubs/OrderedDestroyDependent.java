package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.annotation.PreDestroy;

public class OrderedDestroyDependent {

    @PreDestroy
    public void destroy() {
        LifecycleOrderTracker.record(OrderedDestroyDependent.class, "preDestroy");
    }
}
