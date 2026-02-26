package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

@ApplicationScoped
public class NonResolvableProviderBean {

    @Inject
    public Provider<AuditService> auditServiceProvider;
}
