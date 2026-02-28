package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PostConstructBean {

    public boolean postConstructCalled;
    public boolean preDestroyCalled;

    @PostConstruct
    public void init() {
        postConstructCalled = true;
    }

    @PreDestroy
    public void cleanup() {
        preDestroyCalled = true;
    }
}
