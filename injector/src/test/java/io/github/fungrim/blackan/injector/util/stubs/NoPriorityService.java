package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NoPriorityService implements PriorityService {

    @Override
    public String name() {
        return "none";
    }
}
