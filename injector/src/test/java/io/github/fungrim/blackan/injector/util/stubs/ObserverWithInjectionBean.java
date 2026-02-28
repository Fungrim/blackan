package io.github.fungrim.blackan.injector.util.stubs;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class ObserverWithInjectionBean {

    public final List<String> received = new ArrayList<>();

    public void onStringEvent(@Observes String event, Greeting greeting) {
        received.add(event + ":" + greeting.greet());
    }
}
