package io.github.fungrim.blackan.injector.util.stubs;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class QualifiedEventObserverBean {

    public final List<String> received = new ArrayList<>();

    public void onEvent(@Observes @AppEvent String event) {
        received.add(event);
    }
}
