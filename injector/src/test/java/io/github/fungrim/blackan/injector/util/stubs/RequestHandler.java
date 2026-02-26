package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class RequestHandler {

    @Inject
    public RequestInfo requestInfo;

    @Inject
    public SessionData sessionData;

    @Inject
    public Greeting greeting;
}
