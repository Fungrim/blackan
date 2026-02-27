package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Priority(100)
public class LowPriorityService implements PriorityService {

    @Override
    public String name() {
        return "low";
    }
}
