package io.github.fungrim.blackan.injector.creator.stubs;

import jakarta.inject.Inject;

public class MultipleInjectBean {

    @Inject
    public MultipleInjectBean(String a) {}

    @Inject
    public MultipleInjectBean(String a, String b) {}
}
