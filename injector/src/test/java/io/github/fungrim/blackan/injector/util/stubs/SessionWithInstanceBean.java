package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@SessionScoped
public class SessionWithInstanceBean {

    @Inject
    public Instance<RequestInfo> requestInfoInstance;
}
