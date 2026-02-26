package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;

@SessionScoped
public class SessionService {

    @Inject
    public Greeting greeting;

    public SessionData sessionData;

    @Inject
    public void init(SessionData sessionData) {
        this.sessionData = sessionData;
    }
}
