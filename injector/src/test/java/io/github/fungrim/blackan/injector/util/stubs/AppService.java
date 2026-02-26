package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AppService {

    @Inject
    public Greeting greeting;

    public String hello() {
        return greeting.greet();
    }
}
