package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class LifecycleBeanWithInjection {

    public String greetingValue;

    public void onInitialized(@Observes @Initialized(ApplicationScoped.class) Object event, Greeting greeting) {
        greetingValue = greeting.greet();
    }
}
