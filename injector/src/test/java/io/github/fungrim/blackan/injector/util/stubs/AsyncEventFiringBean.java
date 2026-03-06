package io.github.fungrim.blackan.injector.util.stubs;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@ApplicationScoped
public class AsyncEventFiringBean {

    @Inject
    public Event<String> event;

    @SuppressWarnings("unchecked")
    public CompletionStage<String> fire(String msg) {
        return (CompletionStage<String>) event.fireAsync(msg);
    }
}
