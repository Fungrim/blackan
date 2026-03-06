package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@ApplicationScoped
public class EventFiringBean {

    @Inject
    public Event<String> event;

    public void fire(String msg) {
        event.fire(msg);
    }
}
