package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

@SessionScoped
public class SessionWithProviderBean {

    @Inject
    public Provider<RequestInfo> requestInfoProvider;
}
