package io.github.fungrim.blackan.injector.creator.stubs;

import jakarta.inject.Inject;

public class InjectConstructorBean {

    public final String value;

    @Inject
    public InjectConstructorBean(String value) {
        this.value = value;
    }
}
