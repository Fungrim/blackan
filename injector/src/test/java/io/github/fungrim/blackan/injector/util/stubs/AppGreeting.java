package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AppGreeting implements Greeting {

    @Override
    public String greet() {
        return "hello from app";
    }
}
