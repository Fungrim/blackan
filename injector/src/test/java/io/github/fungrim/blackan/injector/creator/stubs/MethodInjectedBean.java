package io.github.fungrim.blackan.injector.creator.stubs;

import jakarta.inject.Inject;

public class MethodInjectedBean {

    public String value;

    @Inject
    public void init(String value) {
        this.value = value;
    }
}
