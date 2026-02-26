package io.github.fungrim.blackan.injector.stubs;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;

public class InjectionPointHolder {

    @Inject
    @RequestScoped
    public String requestScopedField;

    @Inject
    public String unscopedField;

    @Inject
    @SessionScoped
    public void sessionScopedMethod(String value) {
    }

    @Inject
    public void unscopedMethod(String value) {
    }
}
